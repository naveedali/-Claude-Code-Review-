package com.naveedali.claudecodereview.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── OpenAI Chat Completions response payload ──────────────────────────────────
//
// The API returns a deeply nested structure.  We only deserialise the fields
// we actually use — unknown fields are silently ignored by kotlinx.serialization
// (the default behaviour).  This keeps the DTOs lean and forward-compatible.

/**
 * Top-level response from POST /v1/chat/completions.
 *
 * @param id      Unique identifier for this completion (useful for logging).
 * @param choices List of completion candidates — we always request one (n=1 default).
 * @param usage   Token counts for cost tracking (optional, can be null in streaming).
 */
@Serializable
data class OpenAiResponseDto(
    val id: String,
    val choices: List<ChoiceDto>,
    val usage: UsageDto? = null
)

/**
 * One completion candidate returned by the model.
 *
 * @param message      The assistant's reply.
 * @param finishReason Why the model stopped ("stop", "length", "content_filter").
 */
@Serializable
data class ChoiceDto(
    val message: AssistantMessageDto,
    @SerialName("finish_reason") val finishReason: String? = null
)

/**
 * The assistant turn containing the model's output.
 *
 * [content] holds the raw JSON string when [ResponseFormatDto.type] = "json_object".
 * We pass this string to kotlinx.serialization again to decode it into [ReviewJsonDto].
 */
@Serializable
data class AssistantMessageDto(
    val role: String,
    val content: String
)

/**
 * Token usage stats returned alongside every completion.
 * Useful for monitoring API cost in production.
 */
@Serializable
data class UsageDto(
    @SerialName("prompt_tokens")     val promptTokens: Int,
    @SerialName("completion_tokens") val completionTokens: Int,
    @SerialName("total_tokens")      val totalTokens: Int
)
