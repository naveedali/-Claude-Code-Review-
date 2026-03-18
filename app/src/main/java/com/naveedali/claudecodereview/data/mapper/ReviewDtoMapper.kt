package com.naveedali.claudecodereview.data.mapper

import com.naveedali.claudecodereview.data.remote.dto.IssueDto
import com.naveedali.claudecodereview.data.remote.dto.OptimizationDto
import com.naveedali.claudecodereview.data.remote.dto.ReviewJsonDto
import com.naveedali.claudecodereview.model.CodeIssue
import com.naveedali.claudecodereview.model.IssueSeverity
import com.naveedali.claudecodereview.model.Optimization
import com.naveedali.claudecodereview.model.OptimizationType
import com.naveedali.claudecodereview.model.ReviewResult

// ── ReviewDto → ReviewResult mapper ──────────────────────────────────────────
//
// Why a separate mapper?
// ──────────────────────
// The DTO layer exists to model the API's wire format (snake_case, string enums).
// The domain layer models what our app cares about (typed enums, clean data classes).
// Keeping the conversion in one place means:
//  • If the API changes a field name, only this file changes.
//  • If the domain model changes, only this file changes.
//  • The ViewModel and UI never touch raw API strings.
//
// The mapper is written as top-level extension functions — no class needed,
// easy to call, easy to test in isolation.

/**
 * Converts a [ReviewJsonDto] (raw API output) into a [ReviewResult] (domain model).
 *
 * Defensive mapping:
 *  • Unknown severity strings fall back to [IssueSeverity.INFO] — the app
 *    never crashes because the model hallucinated a bad value.
 *  • Unknown type strings fall back to [OptimizationType.BEST_PRACTICE].
 */
fun ReviewJsonDto.toDomain(): ReviewResult = ReviewResult(
    issues          = issues.map { it.toDomain() },
    optimizations   = optimizations.map { it.toDomain() },
    refactoredCode  = refactoredCode,
    summary         = summary
)

/**
 * Converts a single [IssueDto] into a [CodeIssue].
 *
 * [parseSeverity] is called defensively so an unexpected string from the model
 * (e.g. "CRITICAL" or "error" in lowercase) degrades gracefully to INFO rather
 * than throwing an [IllegalArgumentException].
 */
fun IssueDto.toDomain(): CodeIssue = CodeIssue(
    id          = id,
    severity    = parseSeverity(severity),
    lineNumber  = lineNumber,
    title       = title,
    description = description,
    suggestion  = suggestion
)

/**
 * Converts a single [OptimizationDto] into an [Optimization].
 */
fun OptimizationDto.toDomain(): Optimization = Optimization(
    id          = id,
    type        = parseOptimizationType(type),
    title       = title,
    description = description,
    beforeCode  = beforeCode,
    afterCode   = afterCode
)

// ── Enum parsing helpers ──────────────────────────────────────────────────────

/**
 * Parses a string severity value from the API into an [IssueSeverity] enum.
 *
 * Case-insensitive so "error", "Error", and "ERROR" all work.
 * Falls back to [IssueSeverity.INFO] for any unrecognised value — the user
 * still sees the issue, just with the lowest visual weight.
 */
private fun parseSeverity(raw: String): IssueSeverity =
    when (raw.uppercase()) {
        "ERROR"   -> IssueSeverity.ERROR
        "WARNING" -> IssueSeverity.WARNING
        "INFO"    -> IssueSeverity.INFO
        else      -> IssueSeverity.INFO  // safe fallback for unknown values
    }

/**
 * Parses a string optimization type from the API into an [OptimizationType] enum.
 *
 * Falls back to [OptimizationType.BEST_PRACTICE] for any unrecognised value.
 */
private fun parseOptimizationType(raw: String): OptimizationType =
    when (raw.uppercase()) {
        "PERFORMANCE"   -> OptimizationType.PERFORMANCE
        "READABILITY"   -> OptimizationType.READABILITY
        "MEMORY"        -> OptimizationType.MEMORY
        "BEST_PRACTICE" -> OptimizationType.BEST_PRACTICE
        else            -> OptimizationType.BEST_PRACTICE  // safe fallback
    }
