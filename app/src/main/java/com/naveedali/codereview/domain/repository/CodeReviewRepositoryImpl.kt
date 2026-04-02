package com.naveedali.codereview.domain.repository

import com.naveedali.codereview.domain.analyser.KotlinCodeAnalyser
import com.naveedali.codereview.model.ReviewResult
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
        // AI refactoring is only available when an OpenAI API key is present.
        // The factory (CodeReviewViewModelFactory) automatically picks
        // OpenAiCodeReviewRepository when a key exists in local.properties.
        // Without a key we can't refactor, so we throw a clear error that the
        // ViewModel will surface as a Snackbar message.
        throw UnsupportedOperationException(
            "AI refactoring requires an OpenAI API key.\n" +
            "Add OPENAI_API_KEY=sk-... to local.properties and rebuild."
        )
    }

    private companion object {
        /** Fake delay so the Loading spinner is visible during Phase 2. */
        const val SIMULATED_ANALYSIS_DELAY_MS = 600L
    }
}
