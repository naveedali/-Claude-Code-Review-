package com.naveedali.codereview.model

/**
 * Severity level of a detected code issue.
 *
 * Visual mapping:
 *  ERROR   → red badge    (crash / compilation problem)
 *  WARNING → orange badge (likely bug / bad practice)
 *  INFO    → cyan badge   (style / minor observation)
 */
enum class IssueSeverity {
    ERROR,
    WARNING,
    INFO
}

/**
 * A single issue found in the reviewed code.
 *
 * @param id          Unique identifier (used as LazyColumn key).
 * @param severity    How serious this issue is.
 * @param lineNumber  Optional line in the source where the issue lives.
 * @param title       Short, scannable headline (e.g. "Null pointer risk").
 * @param description Full explanation of why this is a problem.
 * @param suggestion  Optional fix suggestion shown when the card is expanded.
 */
data class CodeIssue(
    val id: String,
    val severity: IssueSeverity,
    val lineNumber: Int? = null,
    val title: String,
    val description: String,
    val suggestion: String? = null
)
