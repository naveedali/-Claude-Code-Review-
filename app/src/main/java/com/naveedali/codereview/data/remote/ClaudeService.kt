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

// ── Anthropic Messages API DTO (private to this file) ─────────────────────────
//
// The Anthropic API has a different shape to OpenAI's Chat Completions:
//  • System prompt is a top-level "system" field (not inside messages[]).
//  • Response content is a list of typed blocks, not a single string.
//  • Auth header is "x-api-key", not "Authorization: Bearer ...".
//  • An extra "anthropic-version" header is required.

@Serializable
private data class ClaudeRequestDto(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int = 2048,
    val system: String,
    val messages: List<ClaudeMessageDto>
)

@Serializable
private data class ClaudeMessageDto(
    val role: String,       // "user" or "assistant"
    val content: String
)

@Serializable
private data class ClaudeResponseDto(
    val id: String,
    val content: List<ClaudeContentBlockDto>,
    @SerialName("stop_reason") val stopReason: String? = null,
    val usage: ClaudeUsageDto? = null
)

@Serializable
private data class ClaudeContentBlockDto(
    val type: String,   // "text" for normal responses
    val text: String = ""
)

@Serializable
private data class ClaudeUsageDto(
    @SerialName("input_tokens")  val inputTokens: Int,
    @SerialName("output_tokens") val outputTokens: Int
)

// ── Service ───────────────────────────────────────────────────────────────────

/**
 * Calls the Anthropic Messages API to review and refactor Kotlin code.
 *
 * Key differences from [OpenAiService]:
 *  • Endpoint: POST https://api.anthropic.com/v1/messages
 *  • Auth: `x-api-key` header (not `Authorization: Bearer`)
 *  • Requires `anthropic-version: 2023-06-01` header
 *  • System prompt is a top-level field, not inside the messages array
 *  • No native JSON mode — we rely on the system prompt to enforce JSON output
 *    and use [Json.isLenient] to tolerate minor formatting quirks
 *
 * @param apiKey The Anthropic API key (starts with "sk-ant-...").
 * @param model  The Claude model to use. Defaults to [MODEL_CLAUDE_OPUS].
 */
class ClaudeService(
    private val apiKey: String,
    private val model: String = MODEL_CLAUDE_OPUS
) : LlmService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)   // Claude can be slower than GPT for long outputs
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true   // Claude has no JSON mode; be lenient with minor formatting
    }

    // ── LlmService implementation ─────────────────────────────────────────────

    override fun review(code: String): ReviewJsonDto =
        executeRequest(code, systemPrompt = SYSTEM_PROMPT_REVIEW)

    override fun refactor(code: String): ReviewJsonDto =
        executeRequest(code, systemPrompt = SYSTEM_PROMPT_REFACTOR)

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun executeRequest(code: String, systemPrompt: String): ReviewJsonDto {
        val requestBody = json.encodeToString(
            ClaudeRequestDto(
                model    = model,
                system   = systemPrompt,
                messages = listOf(
                    ClaudeMessageDto(
                        role    = "user",
                        content = "Review this Kotlin code:\n\n```kotlin\n$code\n```"
                    )
                )
            )
        )

        val request = Request.Builder()
            .url(MESSAGES_URL)
            .header("x-api-key",         apiKey)
            .header("anthropic-version", ANTHROPIC_VERSION)
            .header("Content-Type",      "application/json")
            .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val error = response.body?.string() ?: "No error body"
                throw ClaudeException(response.code, "Claude API error ${response.code}: $error")
            }

            val bodyString = response.body?.string()
                ?: throw IOException("Empty response body from Claude API")

            // Outer decode: extract the text content block from the response
            val outerResponse = json.decodeFromString<ClaudeResponseDto>(bodyString)
            val contentText = outerResponse.content
                .firstOrNull { it.type == "text" }
                ?.text
                ?: throw IOException("No text content block in Claude response")

            // Strip any accidental markdown fences (Claude sometimes adds them
            // despite the system prompt instruction)
            val cleanJson = contentText
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```")
                .trim()

            // Inner decode: parse our ReviewJsonDto from the text
            return json.decodeFromString<ReviewJsonDto>(cleanJson)
        }
    }

    companion object {
        const val MODEL_CLAUDE_OPUS   = "claude-opus-4-5"
        const val MODEL_CLAUDE_SONNET = "claude-sonnet-4-5"
        private const val MESSAGES_URL       = "https://api.anthropic.com/v1/messages"
        private const val ANTHROPIC_VERSION  = "2023-06-01"

        // Same JSON schema as OpenAiService — all three providers share one mapper.
        private val SYSTEM_PROMPT_REVIEW = """
            You are an expert Kotlin code reviewer. Analyse the provided Kotlin code and
            return ONLY a raw JSON object — no markdown, no code fences, no commentary.
            Use exactly this structure:

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
            - Return the raw JSON object only — no markdown code fences
        """.trimIndent()

        private val SYSTEM_PROMPT_REFACTOR = """
            You are an expert Kotlin developer. Refactor the provided Kotlin code and
            return ONLY a raw JSON object — no markdown, no code fences, no commentary:

            {
              "issues": [],
              "optimizations": [],
              "refactored_code": "<complete refactored Kotlin source>",
              "summary": "Code has been refactored."
            }

            Apply Kotlin idioms: val over var, expression bodies, data classes,
            when over if-else chains, safe calls over !!, extension functions.
            Return the raw JSON object only — no markdown code fences.
        """.trimIndent()
    }
}

/** Thrown when the Anthropic API returns a non-2xx status code. */
class ClaudeException(val httpCode: Int, message: String) : IOException(message)
