package com.naveedali.claudecodereview.domain.model

import com.naveedali.claudecodereview.model.ReviewResult

/**
 * All possible states the code-review screen can be in.
 *
 * The ViewModel exposes a [kotlinx.coroutines.flow.StateFlow]<[ReviewUiState]>
 * that the Composable screen collects via collectAsStateWithLifecycle().
 *
 * State machine
 * ─────────────
 *
 *           ┌──────────────────────────────┐
 *           │             Idle             │  ← App start, no review yet
 *           └──────────────┬───────────────┘
 *                          │  user taps "Review Code"
 *                          ▼
 *           ┌──────────────────────────────┐
 *           │           Loading            │  ← Spinner shown, button disabled
 *           └──────┬───────────────┬───────┘
 *          success │               │ exception thrown
 *                  ▼               ▼
 *           ┌───────────┐   ┌──────────────┐
 *           │  Success  │   │    Error     │
 *           └───────────┘   └──────────────┘
 *                  │               │
 *                  └───────┬───────┘
 *                          │ user taps "Review Code" again
 *                          ▼
 *                       Loading …
 *
 * Why sealed class instead of a single data class with nullable fields?
 * ──────────────────────────────────────────────────────────────────────
 * • Each state carries exactly the data it needs — no null checks at the call site.
 * • The `when` expression in the UI is exhaustive, so adding a new state is a
 *   compile error until every consumer handles it.
 * • States are self-documenting — reading the type tells you everything.
 */
sealed class ReviewUiState {

    /** Initial state. The results panel shows a "paste code and tap Review" prompt. */
    data object Idle : ReviewUiState()

    /** A review is in progress. The "Review Code" button shows a spinner. */
    data object Loading : ReviewUiState()

    /**
     * The analyser finished successfully.
     *
     * @param result The [ReviewResult] produced by the analyser.
     */
    data class Success(val result: ReviewResult) : ReviewUiState()

    /**
     * Something went wrong during analysis.
     *
     * @param message Human-readable explanation shown in a Snackbar.
     */
    data class Error(val message: String) : ReviewUiState()
}
