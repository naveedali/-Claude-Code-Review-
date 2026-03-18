package com.naveedali.claudecodereview.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveedali.claudecodereview.domain.model.ReviewUiState
import com.naveedali.claudecodereview.domain.repository.CodeReviewRepository
import com.naveedali.claudecodereview.model.ReviewResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the code-review screen.
 *
 * Responsibilities
 * ────────────────
 *  • Owns [uiState] — a single source of truth for the UI.
 *  • Calls [repository] on [viewModelScope] so work is cancelled when the
 *    ViewModel is cleared (e.g. on back-navigation).
 *  • Survives configuration changes (rotation) — the UI re-collects the same
 *    [StateFlow] after recreation without re-triggering the analysis.
 *
 * StateFlow vs LiveData — why StateFlow?
 * ───────────────────────────────────────
 * • StateFlow is part of Kotlin coroutines — no extra Android dependency.
 * • It always has a current value (no null initial state like LiveData).
 * • It integrates natively with Compose via collectAsStateWithLifecycle().
 * • Transformations (map, combine, filter) are pure functions, easy to test.
 *
 * @param repository The [CodeReviewRepository] providing the analysis.
 *                   Injected so unit tests can pass a fake repository.
 */
class CodeReviewViewModel(
    private val repository: CodeReviewRepository
) : ViewModel() {

    // ── State ─────────────────────────────────────────────────────────────────

    /**
     * Backing mutable state — private so only this ViewModel can mutate it.
     *
     * Naming convention: `_uiState` (private mutable) + `uiState` (public read-only).
     * This is the standard Kotlin StateFlow pattern — the UI can never push state
     * directly; it can only send events via public functions.
     */
    private val _uiState = MutableStateFlow<ReviewUiState>(ReviewUiState.Idle)

    /**
     * Public read-only state observed by the Composable screen.
     *
     * [asStateFlow] wraps the mutable flow in a read-only interface — the compiler
     * prevents the UI from ever casting back to MutableStateFlow.
     */
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    /**
     * Holds the last successful [ReviewResult] so the Refactor action can
     * reference it without re-running the analysis.
     */
    private var lastResult: ReviewResult? = null

    // ── Public events (called by the UI) ─────────────────────────────────────

    /**
     * Triggers analysis of [code] via the repository.
     *
     * Flow:
     *  1. Validates that [code] is not blank.
     *  2. Emits [ReviewUiState.Loading] immediately so the UI shows a spinner.
     *  3. Launches a coroutine on [viewModelScope] — cancelled automatically
     *     when the ViewModel is destroyed.
     *  4. On success: emits [ReviewUiState.Success] with the result.
     *  5. On exception: emits [ReviewUiState.Error] with a human-readable message.
     *
     * @param code Raw Kotlin source text from the editor.
     */
    fun review(code: String) {
        if (code.isBlank()) {
            _uiState.value = ReviewUiState.Error("Please enter some code before running a review.")
            return
        }

        viewModelScope.launch {
            _uiState.value = ReviewUiState.Loading
            _uiState.value = try {
                val result = repository.review(code)
                lastResult  = result
                ReviewUiState.Success(result)
            } catch (e: Exception) {
                ReviewUiState.Error(
                    message = "Analysis failed: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    /**
     * Requests AI-driven refactoring of [code].
     *
     * Phase 2: emits an informational error so the user sees a clear message
     *          rather than a silent no-op.
     * Phase 3: will call [repository].refactor() and emit the refactored code.
     *
     * @param code Raw Kotlin source text to refactor.
     */
    fun refactor(code: String) {
        val result = lastResult
        if (result == null) {
            _uiState.value = ReviewUiState.Error("Run a review first before refactoring.")
            return
        }

        viewModelScope.launch {
            _uiState.value = ReviewUiState.Loading
            _uiState.value = try {
                repository.refactor(code, result)
                // TODO Phase 3: emit a new UiState carrying the refactored code
                ReviewUiState.Success(result)           // fall back to last result for now
            } catch (e: NotImplementedError) {
                // Expected in Phase 2 — surface a friendly message
                ReviewUiState.Error("Refactoring will be available in Phase 3 (AI integration).")
            } catch (e: Exception) {
                ReviewUiState.Error("Refactor failed: ${e.message ?: "Unknown error"}")
            }
        }
    }

    /**
     * Clears a transient [ReviewUiState.Error] after the Snackbar has been shown,
     * returning to the last meaningful state.
     *
     * The UI calls this after [SnackbarHostState.showSnackbar] returns, so the
     * error is not re-shown on the next recomposition.
     */
    fun clearError() {
        if (_uiState.value is ReviewUiState.Error) {
            // Return to Idle if no successful result exists, otherwise Success
            _uiState.value = lastResult
                ?.let  { ReviewUiState.Success(it) }
                ?: ReviewUiState.Idle
        }
    }
}
