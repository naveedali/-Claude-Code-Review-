package com.naveedali.codereview.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveedali.codereview.domain.model.ReviewUiState
import com.naveedali.codereview.domain.repository.CodeReviewRepository
import com.naveedali.codereview.model.ReviewResult
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
     * Phase 3 behaviour:
     *  1. Emits [ReviewUiState.Loading].
     *  2. Calls [repository.refactor] — hits GPT-4o with a focused refactoring prompt.
     *  3. Merges the returned string into a copy of [lastResult] so the issue list
     *     is preserved alongside the new refactored source.
     *  4. Emits [ReviewUiState.Success] with the updated [ReviewResult].
     *
     * The UI detects [ReviewResult.refactoredCode] != null and offers a
     * confirmation dialog — "Apply refactored code to editor?".
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
                // Phase 3: repository.refactor() returns the improved Kotlin source
                val refactoredCode = repository.refactor(code, result)

                // Merge into the existing result — the issue list is preserved so
                // the user still sees the review findings alongside the refactored code.
                val updatedResult = result.copy(refactoredCode = refactoredCode)
                lastResult = updatedResult
                ReviewUiState.Success(updatedResult)
            } catch (e: UnsupportedOperationException) {
                // Thrown by CodeReviewRepositoryImpl when no API key is configured.
                // Surface a helpful instruction rather than a raw exception message.
                ReviewUiState.Error(
                    "AI refactoring needs an OpenAI API key. " +
                    "Add OPENAI_API_KEY=sk-... to local.properties and rebuild."
                )
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
