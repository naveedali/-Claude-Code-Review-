package com.naveedali.claudecodereview.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── OpenAI Chat Completions request payload ───────────────────────────────────
//
// These data classes are annotated with @Serializable so kotlinx.serialization
// can convert them to JSON automatically — no manual string building needed.
//
// Key design decisions:
//  • @SerialName maps Kotlin camelCase names to the snake_case the API expects.
//  • Every class is a simple data class — immutable, easy to construct, easy to test.
//  • The request wrapper is minimal: only the fields OpenAI requires for our use case.

/**
 * Top-level body sent to POST /v1/chat/completions.
 *
 * @param model          The GPT model to use (e.g. "gpt-4o").
 * @param messages       Conversation history — we always send system + user.
 * @param responseFormat Instructs the model to reply with valid JSON.
 * @param maxTokens      Caps response size. 2048 is plenty for a code review.
 * @param temperature    0.2 = mostly deterministic output (good for structured data).
 */
@Serializable
data class OpenAiRequestDto(
    val model: String,
    val messages: List<MessageDto>,
    @SerialName("response_format") val responseFormat: ResponseFormatDto = ResponseFormatDto(),
    @SerialName("max_tokens")      val maxTokens: Int = 2048,
    val temperature: Double = 0.2
)

/**
 * A single turn in the conversation.
 *
 * @param role    "system" (instructions) or "user" (the code to review).
 * @param content The text content of this message.
 */
@Serializable
data class MessageDto(
    val role: String,
    val content: String
)

/**
 * Tells the OpenAI API to always return a valid JSON object.
 *
 * Setting type = "json_object" guarantees the content field of the response
 * is parseable JSON — no need to strip markdown fences or handle partial output.
 */
@Serializable
data class ResponseFormatDto(
    val type: String = "json_object"
)
