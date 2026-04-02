package com.naveedali.claudecodereview.data.remote

import com.naveedali.claudecodereview.data.remote.dto.MessageDto
import com.naveedali.claudecodereview.data.remote.dto.OpenAiRequestDto
import com.naveedali.claudecodereview.data.remote.dto.OpenAiResponseDto
import com.naveedali.claudecodereview.data.remote.dto.ReviewJsonDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin wrapper around the OpenAI Chat Completions API.
 *
 * Responsibilities
 * ────────────────
 *  • Builds the HTTP request (headers, JSON body).
 *  • Executes the request on whatever thread the caller provides
 *    (no coroutine dispatcher logic here — that lives in the repository).
 *  • Parses the raw JSON response into a [ReviewJsonDto].
 *  • Throws [IOException] on network failure or [OpenAiException] on API errors.
 *
 * Why OkHttp instead of Retrofit?
 * ────────────────────────────────
 * OkHttp is more explicit and educational — you see every header, every body.
 * Retrofit would hide the boilerplate behind an interface, which is great for
 * production but less clear for learning.  Phase 4 could migrate to Retrofit.
 *
 * @param apiKey The OpenAI secret key from BuildConfig.OPENAI_API_KEY.
 * @param model  The GPT model to use. Defaults to "gpt-4o".
 */
class OpenAiService(
    private val apiKey: String,
    private val model: String = MODEL_GPT_4O
) : LlmService {

    // ── HTTP client ───────────────────────────────────────────────────────────

    /**
     * Shared OkHttpClient instance.
     *
     * Timeouts:
     *  • connect = 15 s — waiting to establish a TCP connection
     *  • read    = 60 s — waiting for the full API response (GPT-4o can be slow)
     *
     * Interceptors:
     *  • [HttpLoggingInterceptor] prints request/response details in BODY level.
     *    In production you'd guard this with BuildConfig.DEBUG.
     *    Logs look like: --> POST https://api.openai.com/v1/chat/completions
     */
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        )
        .build()

    // ── JSON codec ────────────────────────────────────────────────────────────

    /**
     * kotlinx.serialization Json instance.
     *
     * [ignoreUnknownKeys] = true means the decoder won't crash if OpenAI adds
     * new fields to the response — our DTOs stay stable across API versions.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient         = true  // tolerates minor JSON quirks from the model
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Sends [code] to GPT-4o and returns a [ReviewJsonDto] with issues,
     * optimizations, refactored code, and a summary.
     *
     * This is a blocking call — the caller (repository) must wrap it in
     * `withContext(Dispatchers.IO)`.
     *
     * @param code The Kotlin source text to review.
     * @throws IOException      On network error (no connection, timeout, etc.).
     * @throws OpenAiException  When the API returns an HTTP error code.
     * @throws RuntimeException When JSON parsing fails unexpectedly.
     */
    override fun review(code: String): ReviewJsonDto {
        val requestBody = buildRequestBody(code, mode = "review")
        return executeRequest(requestBody)
    }

    /**
     * Sends [code] to GPT-4o and returns a [ReviewJsonDto] where
     * [ReviewJsonDto.refactoredCode] contains the improved version.
     *
     * @param code The Kotlin source text to refactor.
     * @throws IOException      On network error.
     * @throws OpenAiException  On API error.
     */
    override fun refactor(code: String): ReviewJsonDto {
        val requestBody = buildRequestBody(code, mode = "refactor")
        return executeRequest(requestBody)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Builds the JSON body for the Chat Completions request.
     *
     * Prompt engineering choices:
     *  • System message defines the assistant's role and output schema precisely.
     *  • [response_format] = json_object forces valid JSON output — no markdown fences.
     *  • temperature = 0.2 makes responses more consistent across identical inputs.
     *  • The schema in the system message mirrors [ReviewJsonDto] exactly so the
     *    model knows the expected field names and types.
     *
     * @param code The source code snippet from the user.
     * @param mode "review" for full analysis, "refactor" for refactoring only.
     */
    private fun buildRequestBody(code: String, mode: String): String {
        val systemPrompt = if (mode == "review") SYSTEM_PROMPT_REVIEW else SYSTEM_PROMPT_REFACTOR

        val request = OpenAiRequestDto(
            model    = model,
            messages = listOf(
                MessageDto(role = "system", content = systemPrompt),
                MessageDto(role = "user",   content = "Review this Kotlin code:\n\n```kotlin\n$code\n```")
            )
        )

        // Serialise to JSON string using kotlinx.serialization
        // The @Serializable annotation on OpenAiRequestDto enables this one-liner.
        return json.encodeToString(request)
    }

    /**
     * Executes a blocking HTTP POST to the Chat Completions endpoint.
     *
     * Flow:
     *  1. Wrap [bodyJson] in an OkHttp [RequestBody] with Content-Type application/json.
     *  2. Build a [Request] with the Authorization and Content-Type headers.
     *  3. Execute synchronously — OkHttp handles the thread.
     *  4. Check the HTTP status code; throw [OpenAiException] on error.
     *  5. Decode the outer [OpenAiResponseDto] wrapper.
     *  6. Extract the `content` string from the first choice.
     *  7. Decode the inner [ReviewJsonDto] from that content string.
     *
     * @param bodyJson Serialised [OpenAiRequestDto] JSON string.
     */
    private fun executeRequest(bodyJson: String): ReviewJsonDto {
        // Step 1 & 2 — build the OkHttp Request
        val mediaType   = "application/json; charset=utf-8".toMediaType()
        val requestBody = bodyJson.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(COMPLETIONS_URL)
            .header("Authorization",  "Bearer $apiKey")
            .header("Content-Type",   "application/json")
            .post(requestBody)
            .build()

        // Step 3 — execute (blocking)
        // use{} automatically closes the response body when the lambda exits,
        // preventing connection leaks.
        httpClient.newCall(request).execute().use { response ->

            // Step 4 — check HTTP status
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "No error body"
                throw OpenAiException(
                    httpCode  = response.code,
                    message   = "OpenAI API error ${response.code}: $errorBody"
                )
            }

            // Step 5 — decode the outer wrapper
            val responseBodyString = response.body?.string()
                ?: throw IOException("Empty response body from OpenAI")

            val outerResponse: OpenAiResponseDto = json.decodeFromString(responseBodyString)

            // Step 6 — extract the content string (this is our inner JSON)
            val contentJson = outerResponse.choices
                .firstOrNull()
                ?.message
                ?.content
                ?: throw IOException("No content in OpenAI response choices")

            // Step 7 — decode the inner ReviewJsonDto
            // If the model hallucinated a bad schema, this will throw a
            // SerializationException — the repository maps it to an Error state.
            return json.decodeFromString<ReviewJsonDto>(contentJson)
        }
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        const val MODEL_GPT_4O  = "gpt-4o"
        const val COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions"

        // ── System prompts ────────────────────────────────────────────────────
        //
        // These prompts are the core of the Phase 3 feature.  They instruct the
        // model to act as a Kotlin expert and always return the exact JSON schema
        // our ReviewJsonDto expects.  Clear schema description = fewer parsing errors.

        private val SYSTEM_PROMPT_REVIEW = """
            You are an expert Kotlin code reviewer. Analyse the provided Kotlin code and
            return a JSON object with EXACTLY this structure — no extra keys, no markdown:

            {
              "issues": [
                {
                  "id": "issue_1",
                  "severity": "ERROR | WARNING | INFO",
                  "line_number": <integer or null>,
                  "title": "<short headline>",
                  "description": "<full explanation>",
                  "suggestion": "<optional fix>"
                }
              ],
              "optimizations": [
                {
                  "id": "opt_1",
                  "type": "PERFORMANCE | READABILITY | MEMORY | BEST_PRACTICE",
                  "title": "<short headline>",
                  "description": "<why it matters>",
                  "before_code": "<optional snippet>",
                  "after_code": "<optional improved snippet>"
                }
              ],
              "refactored_code": "<complete refactored Kotlin source as a single string>",
              "summary": "<one-sentence summary like: 2 errors, 1 warning · 3 optimizations>"
            }

            Rules:
            - severity must be exactly ERROR, WARNING, or INFO (uppercase)
            - type must be exactly PERFORMANCE, READABILITY, MEMORY, or BEST_PRACTICE
            - ids must be unique strings like issue_1, issue_2, opt_1, opt_2
            - refactored_code must be the complete improved Kotlin source, not a diff
            - Return only the JSON object, no markdown code fences
        """.trimIndent()

        private val SYSTEM_PROMPT_REFACTOR = """
            You are an expert Kotlin developer. Refactor the provided Kotlin code following
            Kotlin idioms, best practices, and clean code principles.
            Return a JSON object with EXACTLY this structure — no extra keys, no markdown:

            {
              "issues": [],
              "optimizations": [],
              "refactored_code": "<complete refactored Kotlin source>",
              "summary": "Code has been refactored."
            }

            Rules:
            - refactored_code must be the complete refactored Kotlin source, not a diff
            - Preserve the original logic and public API
            - Apply: val over var, expression bodies, extension functions, data classes,
              when over if-else chains, nullable safety, coroutine-friendly patterns
            - Return only the JSON object, no markdown code fences
        """.trimIndent()
    }
}

// ── Custom exception ──────────────────────────────────────────────────────────

/**
 * Thrown when the OpenAI API returns a non-2xx HTTP status code.
 *
 * Common codes:
 *  • 401 — invalid or missing API key
 *  • 429 — rate limit exceeded
 *  • 500 — OpenAI server error
 *
 * @param httpCode The HTTP status code returned by the API.
 * @param message  Human-readable description including the error body.
 */
class OpenAiException(val httpCode: Int, message: String) : IOException(message)
