package com.naveedali.codereview.data.remote

import com.naveedali.codereview.data.mapper.toDomain
import com.naveedali.codereview.domain.repository.CodeReviewRepository
import com.naveedali.codereview.model.ReviewResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Generic AI-backed [CodeReviewRepository].
 *
 * This replaces the provider-specific [OpenAiCodeReviewRepository] with a single
 * class that works with any [LlmService] implementation — OpenAI, Claude, or Gemini.
 *
 * The factory ([com.naveedali.codereview.presentation.CodeReviewViewModelFactory])
 * reads the user's selection from [com.naveedali.codereview.data.preferences.LlmPreferences]
 * and injects the appropriate service here.  Swapping providers is a factory concern
 * — this class never needs to change.
 *
 * Threading
 * ─────────
 * All three services use OkHttp's blocking `execute()`.  Wrapping each call in
 * `withContext(Dispatchers.IO)` moves the blocking work off the main thread while
 * allowing the ViewModel's `viewModelScope` to manage cancellation.
 *
 * @param service Any [LlmService] — [OpenAiService], [ClaudeService], or [GeminiService].
 */
class AiCodeReviewRepository(
    private val service: LlmService
) : CodeReviewRepository {

    override suspend fun review(code: String): ReviewResult =
        withContext(Dispatchers.IO) {
            service.review(code).toDomain()
        }

    override suspend fun refactor(code: String, reviewResult: ReviewResult): String =
        withContext(Dispatchers.IO) {
            val dto = service.refactor(code)
            dto.refactoredCode
                ?: throw RuntimeException("Model did not return a refactored_code field.")
        }
}
