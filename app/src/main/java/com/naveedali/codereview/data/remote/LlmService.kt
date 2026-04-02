package com.naveedali.codereview.data.remote

import com.naveedali.codereview.data.remote.dto.ReviewJsonDto

/**
 * Common interface for all AI provider services.
 *
 * Why an interface?
 * ─────────────────
 * Each provider (OpenAI, Claude, Gemini) has a completely different HTTP API —
 * different endpoints, headers, and JSON schemas.  The interface hides that
 * complexity behind two simple methods.  The [AiCodeReviewRepository] works
 * with any implementation without knowing which provider is active.
 *
 * Blocking vs suspend
 * ───────────────────
 * Methods are intentionally blocking (not suspend).  Each service uses
 * OkHttp's synchronous `execute()` and is called from inside a
 * `withContext(Dispatchers.IO)` block in the repository — keeping the
 * threading concern in one place.
 *
 * Both methods return [ReviewJsonDto] so a single mapper converts any
 * provider's output into the domain [com.naveedali.codereview.model.ReviewResult].
 */
interface LlmService {

    /**
     * Sends [code] to the AI for a full review.
     *
     * The implementation must return a [ReviewJsonDto] whose [ReviewJsonDto.issues],
     * [ReviewJsonDto.optimizations], [ReviewJsonDto.refactoredCode], and
     * [ReviewJsonDto.summary] are populated.
     *
     * @throws java.io.IOException On network failure.
     * @throws Exception           On parsing or API error.
     */
    fun review(code: String): ReviewJsonDto

    /**
     * Sends [code] to the AI for a focused refactoring pass.
     *
     * The implementation should populate only [ReviewJsonDto.refactoredCode]
     * (issues and optimizations may be empty).
     *
     * @throws java.io.IOException On network failure.
     * @throws Exception           On parsing or API error.
     */
    fun refactor(code: String): ReviewJsonDto
}
