package com.naveedali.codereview.data.remote

import com.naveedali.codereview.data.mapper.toDomain
import com.naveedali.codereview.domain.repository.CodeReviewRepository
import com.naveedali.codereview.model.ReviewResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Phase 3 implementation of [CodeReviewRepository].
 *
 * Delegates to [OpenAiService] (GPT-4o via OkHttp) then maps the response
 * DTO into domain models via [ReviewDtoMapper].
 *
 * Architecture note — where does this class live?
 * ────────────────────────────────────────────────
 * It sits in the `data` layer alongside [OpenAiService].  The domain layer
 * (interface) knows nothing about OpenAI, OkHttp, or JSON — it only sees
 * [ReviewResult].  This separation means:
 *  • Swapping to a different AI provider = new class, same interface.
 *  • Unit-testing the ViewModel = inject a fake [CodeReviewRepository].
 *
 * Threading
 * ─────────
 * [OpenAiService] methods are blocking — they use OkHttp's synchronous
 * `execute()`.  We wrap each call in `withContext(Dispatchers.IO)` here so
 * the ViewModel can call these suspend functions safely from [viewModelScope].
 * [Dispatchers.IO] is designed for blocking I/O (network, disk) — it keeps
 * a pool of threads available for exactly this purpose.
 *
 * @param service The [OpenAiService] that makes the actual HTTP call.
 *                Injected so tests can supply a fake service.
 */
class OpenAiCodeReviewRepository(
    private val service: OpenAiService
) : CodeReviewRepository {

    /**
     * Sends [code] to GPT-4o and returns a [ReviewResult] with detected issues,
     * optimizations, the refactored version, and a plain-English summary.
     *
     * @param code Raw Kotlin source text from the editor.
     * @throws IOException      On network failure (caught by ViewModel → Error state).
     * @throws OpenAiException  On API error (401, 429, 500 …).
     */
    override suspend fun review(code: String): ReviewResult =
        withContext(Dispatchers.IO) {
            // service.review() is blocking — safe here because we're on IO dispatcher
            val dto = service.review(code)
            // Map the raw DTO to a clean domain ReviewResult
            dto.toDomain()
        }

    /**
     * Sends [code] to GPT-4o for refactoring and returns a [ReviewResult] where
     * [ReviewResult.refactoredCode] contains the improved Kotlin source.
     *
     * The full [ReviewResult] is returned (not just the string) so the ViewModel
     * can emit it as a [ReviewUiState.Success] — keeping the state model uniform.
     *
     * @param code         The original Kotlin source to refactor.
     * @param reviewResult The previously produced analysis (not used by GPT here,
     *                     but kept in the signature to satisfy the interface — the
     *                     system prompt covers refactoring rules independently).
     */
    override suspend fun refactor(code: String, reviewResult: ReviewResult): String =
        withContext(Dispatchers.IO) {
            val dto = service.refactor(code)
            // The refactor prompt produces an empty issues/optimizations list
            // and populates only refactored_code — we extract just that string.
            dto.refactoredCode
                ?: throw RuntimeException("Model did not return a refactored_code field.")
        }
}
