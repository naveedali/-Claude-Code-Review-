package com.naveedali.codereview.model

/**
 * The complete result returned after analysing a code snippet.
 *
 * Phase 1 — all fields are populated with mock data so the UI can be
 * built and previewed without any AI integration.
 *
 * @param issues          List of detected bugs / problems.
 * @param optimizations   List of improvement suggestions.
 * @param refactoredCode  Optional AI-refactored version (Phase 2+).
 * @param summary         One-line overview shown at the top of the panel.
 */
data class ReviewResult(
    val issues: List<CodeIssue>       = emptyList(),
    val optimizations: List<Optimization> = emptyList(),
    val refactoredCode: String?        = null,
    val summary: String               = ""
)

// ── Mock data for UI previews ─────────────────────────────────────────────────

val mockIssues = listOf(
    CodeIssue(
        id          = "issue_1",
        severity    = IssueSeverity.ERROR,
        lineNumber  = 14,
        title       = "Null pointer dereference",
        description = "The variable `user` may be null at this point. " +
                      "Calling `.name` without a null check will crash at runtime.",
        suggestion  = "Use safe-call: `user?.name ?: \"Unknown\"`"
    ),
    CodeIssue(
        id          = "issue_2",
        severity    = IssueSeverity.WARNING,
        lineNumber  = 27,
        title       = "Mutable list exposed publicly",
        description = "`_items` is a MutableList returned from a public function. " +
                      "Callers can modify internal state directly.",
        suggestion  = "Return `List<T>` instead: `fun items(): List<Item> = _items`"
    ),
    CodeIssue(
        id          = "issue_3",
        severity    = IssueSeverity.WARNING,
        lineNumber  = 42,
        title       = "Magic number without context",
        description = "The value `86400` is used directly. " +
                      "This is hard to understand without the surrounding context.",
        suggestion  = "Replace with a named constant: `const val SECONDS_IN_DAY = 86_400`"
    ),
    CodeIssue(
        id          = "issue_4",
        severity    = IssueSeverity.INFO,
        lineNumber  = 58,
        title       = "Unused import",
        description = "`import java.util.Date` is imported but never referenced in this file.",
        suggestion  = "Remove the import to keep the file tidy."
    )
)

val mockOptimizations = listOf(
    Optimization(
        id          = "opt_1",
        type        = OptimizationType.PERFORMANCE,
        title       = "Use sequence for large list processing",
        description = "Chaining `filter` and `map` on a plain List creates intermediate " +
                      "collections. A Sequence processes elements lazily, saving allocations.",
        beforeCode  = "list.filter { it.active }.map { it.name }",
        afterCode   = "list.asSequence().filter { it.active }.map { it.name }.toList()"
    ),
    Optimization(
        id          = "opt_2",
        type        = OptimizationType.READABILITY,
        title       = "Replace if-else chain with `when`",
        description = "A series of `if/else if` checks on the same variable is verbose. " +
                      "`when` is idiomatic Kotlin and easier to read.",
        beforeCode  = "if (x == 1) a()\nelse if (x == 2) b()\nelse c()",
        afterCode   = "when (x) {\n    1 -> a()\n    2 -> b()\n    else -> c()\n}"
    ),
    Optimization(
        id          = "opt_3",
        type        = OptimizationType.MEMORY,
        title       = "Avoid creating objects inside a loop",
        description = "A new `StringBuilder` is instantiated on every iteration. " +
                      "Move the allocation outside the loop and call `clear()` between uses.",
        beforeCode  = "for (item in items) {\n    val sb = StringBuilder()\n    …\n}",
        afterCode   = "val sb = StringBuilder()\nfor (item in items) {\n    sb.clear()\n    …\n}"
    ),
    Optimization(
        id          = "opt_4",
        type        = OptimizationType.BEST_PRACTICE,
        title       = "Prefer `data class` for plain holders",
        description = "This class holds state and overrides `equals`/`hashCode` manually. " +
                      "A `data class` generates them automatically and adds `copy()` for free.",
        beforeCode  = null,
        afterCode   = null
    )
)

/** Pre-built mock result used in all Compose previews. */
val mockReviewResult = ReviewResult(
    issues        = mockIssues,
    optimizations = mockOptimizations,
    summary       = "Found ${mockIssues.size} issues · ${mockOptimizations.size} optimizations"
)
