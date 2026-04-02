package com.naveedali.codereview.data.remote

import com.naveedali.codereview.data.remote.dto.ReviewJsonDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

// ── Google Gemini API DTOs (private to this file) ─────────────────────────────
//
// The Gemini generateContent API uses a "contents" array where each item has
// a "role" and a "parts" array.  The system prompt goes in a separate
// "systemInstruction" field (similar to Claude, different from OpenAI).
//
// JSON mode is enabled via generationConfig.responseMimeType = "application/json".

@Serializable
private data class GeminiRequestDto(
    @SerialName("systemInstruction") val systemInstruction: GeminiContentDto,
    val contents: List<GeminiContentDto>,
    @SerialName("generationConfig") val generationConfig: GeminiGenerationConfigDto
)

@Serializable
private data class GeminiContentDto(
    val role: String = "user",
    val parts: List<GeminiPartDto>
)

@Serializable
private data class GeminiPartDto(
    val text: String
)

@Serializable
private data class GeminiGenerationConfigDto(
    // Forces the model to return a valid JSON object — Gemini's equivalent
    // of OpenAI's response_format: { type: "json_object" }
    @SerialName("responseMimeType") val responseMimeType: String = "application/json"
)

@Serializable
private data class GeminiResponseDto(
    val candidates: List<GeminiCandidateDto> = emptyList()
)

@Serializable
private data class GeminiCandidateDto(
    val content: GeminiContentDto? = null,
    @SerialName("finishReason") val finishReason: String? = null
)

// ── Service ───────────────────────────────────────────────────────────────────

/**
 * Calls the Google Gemini generateContent API to review and refactor Kotlin code.
 *
 * Key differences from [OpenAiService] and [ClaudeService]:
 *  • Endpoint: POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={apiKey}
 *  • Auth: API key is a query parameter (`?key=...`), not a header
 *  • System prompt goes in a top-level "systemInstruction" object
 *  • JSON mode: set generationConfig.responseMimeType = "application/json"
 *  • Response content is nested inside candidates[0].content.parts[0].text
 *
 * @param apiKey The Google API key (starts with "AIza...").
 * @param model  The Gemini model to use. Defaults to [MODEL_GEMINI_PRO].
 */
class GeminiService(
    private val apiKey: String,
    private val model: String = MODEL_GEMINI_PRO
) : LlmService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // ── LlmService implementation ─────────────────────────────────────────────

    override fun review(code: String): ReviewJsonDto =
        executeRequest(code, systemPrompt = SYSTEM_PROMPT_REVIEW)

    override fun refactor(code: String): ReviewJsonDto =
        executeRequest(code, systemPrompt = SYSTEM_PROMPT_REFACTOR)

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Builds and executes the Gemini generateContent request.
     *
     * The API key is passed as a query parameter — this is the standard Gemini
     * auth pattern (unlike OpenAI/Claude which use auth headers).
     */
    private fun executeRequest(code: String, systemPrompt: String): ReviewJsonDto {
        // Build URL with API key as query param
        val url = "$BASE_URL$model:generateContent?key=$apiKey"

        val requestBody = json.encodeToString(
            GeminiRequestDto(
                systemInstruction = GeminiContentDto(
                    role  = "user",   // systemInstruction role is ignored but required
                    parts = listOf(GeminiPartDto(text = systemPrompt))
                ),
                contents = listOf(
                    GeminiContentDto(
                        role  = "user",
                        parts = listOf(
                            GeminiPartDto(
                                text = "Review this Kotlin code:\n\n```kotlin\n$code\n```"
                            )
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfigDto()
            )
        )

        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val error = response.body?.string() ?: "No error body"
                throw GeminiException(response.code, "Gemini API error ${response.code}: $error")
            }

            val bodyString = response.body?.string()
                ?: throw IOException("Empty response body from Gemini API")

            // Outer decode: navigate the nested Gemini response structure
            val outerResponse = json.decodeFromString<GeminiResponseDto>(bodyString)
            val contentText = outerResponse.candidates
                .firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?: throw IOException("No content in Gemini response candidates")

            // With responseMimeType = "application/json", Gemini returns clean JSON.
            // Still strip fences defensively.
            val cleanJson = contentText
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```")
                .trim()

            return json.decodeFromString<ReviewJsonDto>(cleanJson)
        }
    }

    companion object {
        const val MODEL_GEMINI_PRO   = "gemini-1.5-pro"
        const val MODEL_GEMINI_FLASH = "gemini-1.5-flash"  // faster, cheaper
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

        // Same schema as OpenAI/Claude — all three share the ReviewDtoMapper.
        private val SYSTEM_PROMPT_REVIEW = """
            You are an expert Kotlin code reviewer. Analyse the provided Kotlin code and
            return a JSON object with EXACTLY this structure:

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
              "refactored_code": "<complete refactored Kotlin source>",
              "summary": "<one-sentence summary>"
            }

            Rules:
            - severity must be ERROR, WARNING, or INFO (uppercase)
            - type must be PERFORMANCE, READABILITY, MEMORY, or BEST_PRACTICE
            - refactored_code must be the complete improved source, not a diff
        """.trimIndent()

        private val SYSTEM_PROMPT_REFACTOR = """
            You are an expert Kotlin developer. Refactor the provided Kotlin code following
            Kotlin idioms and best practices. Return a JSON object:

            {
              "issues": [],
              "optimizations": [],
              "refactored_code": "<complete refactored Kotlin source>",
              "summary": "Code has been refactored."
            }

            Apply: val over var, expression bodies, data classes, when over if-else,
            safe calls over !!, extension functions, coroutine-friendly patterns.
        """.trimIndent()
    }
}

/** Thrown when the Gemini API returns a non-2xx status code. */
class GeminiException(val httpCode: Int, message: String) : IOException(message)
