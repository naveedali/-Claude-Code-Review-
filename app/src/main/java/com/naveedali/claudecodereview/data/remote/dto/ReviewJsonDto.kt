package com.naveedali.claudecodereview.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Inner JSON structure returned by the model ────────────────────────────────
//
// This is the schema we describe to GPT-4o in the system prompt.
// Using snake_case field names here so the prompt and the DTOs stay in sync —
// the model will use the same names it is instructed to produce.

/**
 * Root object the model returns inside the "content" string.
 *
 * The mapper converts this into a [com.naveedali.claudecodereview.model.ReviewResult].
 *
 * @param issues          Zero or more issues found in the code.
 * @param optimizations   Zero or more improvement suggestions.
 * @param refactoredCode  A clean, refactored version of the entire code snippet.
 * @param summary         One-sentence overview (e.g. "3 issues, 2 optimizations").
 */
@Serializable
data class ReviewJsonDto(
    val issues: List<IssueDto>              = emptyList(),
    val optimizations: List<OptimizationDto> = emptyList(),
    @SerialName("refactored_code") val refactoredCode: String? = null,
    val summary: String                      = ""
)

/**
 * A single code issue as returned by the model.
 *
 * Field names mirror [com.naveedali.claudecodereview.model.CodeIssue] but use
 * snake_case to match the JSON the model produces.
 *
 * @param id          Unique identifier (model generates "issue_1", "issue_2", …).
 * @param severity    One of: "ERROR", "WARNING", "INFO".
 * @param lineNumber  Nullable — the model may not always know the exact line.
 * @param title       Short headline for the issue card.
 * @param description Full explanation of the problem.
 * @param suggestion  Optional fix hint.
 */
@Serializable
data class IssueDto(
    val id: String,
    val severity: String,
    @SerialName("line_number") val lineNumber: Int? = null,
    val title: String,
    val description: String,
    val suggestion: String? = null
)

/**
 * A single optimization suggestion from the model.
 *
 * @param id          Unique identifier (model generates "opt_1", "opt_2", …).
 * @param type        One of: "PERFORMANCE", "READABILITY", "MEMORY", "BEST_PRACTICE".
 * @param title       Short headline for the optimization card.
 * @param description Why this optimization matters.
 * @param beforeCode  Optional snippet of the current (worse) code.
 * @param afterCode   Optional snippet of the improved code.
 */
@Serializable
data class OptimizationDto(
    val id: String,
    val type: String,
    val title: String,
    val description: String,
    @SerialName("before_code") val beforeCode: String? = null,
    @SerialName("after_code")  val afterCode: String?  = null
)
