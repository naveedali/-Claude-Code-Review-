package com.naveedali.claudecodereview.domain.repository

import com.naveedali.claudecodereview.domain.analyser.KotlinCodeAnalyser
import com.naveedali.claudecodereview.model.ReviewResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Phase 2 implementation of [CodeReviewRepository].
 *
 * Delegates analysis to [KotlinCodeAnalyser] (local, rule-based).
 *
 * Threading
 * ─────────
 * [review] switches to [Dispatchers.Default] — the analyser is CPU-bound
 * (regex matching across the code string) and should not run on the Main thread.
 *
 * Simulated delay
 * ────────────────
 * A 600 ms delay is added so the Loading state is visible in the UI.
 * This will be replaced in Phase 3 by an actual network round-trip.
 * Remove [delay] (and the import) once the AI call is wired up.
 *
 * @param analyser The [KotlinCodeAnalyser] to use. Injected so it can be
 *                 replaced with a test double in unit tests.
 */
class CodeReviewRepositoryImpl(
    private val analyser: KotlinCodeAnalyser
) : CodeReviewRepository {

    override suspend fun review(code: String): ReviewResult =
        withContext(Dispatchers.Default) {
            // TODO Phase 3: remove simulated delay once real AI call is in place
            delay(SIMULATED_ANALYSIS_DELAY_MS)
            analyser.analyse(code)
        }

    override suspend fun refactor(code: String, reviewResult: ReviewResult): String {
        // TODO Phase 3: replace with AI service call
        //   return aiService.refactor(code, reviewResult.issues, reviewResult.optimizations)
        throw NotImplementedError(
            "Refactoring requires Phase 3 AI integration. " +
            "Wire up an AI service in CodeReviewRepositoryImpl.refactor()."
        )
    }

    private companion object {
        /** Fake delay so the Loading spinner is visible during Phase 2. */
        const val SIMULATED_ANALYSIS_DELAY_MS = 600L
    }
}
