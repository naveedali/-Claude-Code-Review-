package com.naveedali.claudecodereview.domain.repository

import com.naveedali.claudecodereview.model.ReviewResult

/**
 * Contract for the code-review data source.
 *
 * Why an interface?
 * ─────────────────
 * The ViewModel depends on this interface, not on a concrete class.
 * This means:
 *  • Phase 2 — [CodeReviewRepositoryImpl] runs the local rule-based analyser.
 *  • Phase 3 — A new `AiCodeReviewRepository` hits an AI service.
 *
 * Swapping implementations requires changing one line in [CodeReviewViewModelFactory]
 * (or a DI binding), with zero changes to the ViewModel or the UI.
 *
 * All functions are `suspend` so the ViewModel can call them inside
 * `viewModelScope.launch {}` without worrying about threading.
 */
interface CodeReviewRepository {

    /**
     * Analyses [code] and returns a [ReviewResult] with all detected issues
     * and optimisation suggestions.
     *
     * Implementations are responsible for switching to an appropriate
     * dispatcher (e.g. [kotlinx.coroutines.Dispatchers.Default] for CPU work,
     * [kotlinx.coroutines.Dispatchers.IO] for network calls).
     *
     * @param code Raw Kotlin source text to analyse.
     * @throws Exception if the analysis fails (network error, timeout, etc.).
     *                   The ViewModel maps this to [com.naveedali.claudecodereview.domain.model.ReviewUiState.Error].
     */
    suspend fun review(code: String): ReviewResult

    /**
     * Returns a refactored version of [code] based on [reviewResult].
     *
     * Phase 2: Not yet implemented — throws [NotImplementedError].
     * Phase 3: Will delegate to an AI service.
     *
     * @param code         Original source code.
     * @param reviewResult The previously produced [ReviewResult] (context for the AI).
     * @return             Refactored Kotlin source code as a String.
     */
    suspend fun refactor(code: String, reviewResult: ReviewResult): String
}
