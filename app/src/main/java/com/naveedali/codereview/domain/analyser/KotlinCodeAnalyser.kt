package com.naveedali.codereview.domain.analyser

import com.naveedali.codereview.model.IssueSeverity
import com.naveedali.codereview.model.ReviewResult

/**
 * Orchestrates all [IssueRules] and [OptimizationRules] against a Kotlin
 * source snippet and produces a [ReviewResult].
 *
 * This class is the single entry point for the domain analyser layer.
 * The repository calls [analyse] on a background dispatcher — keeping this
 * function synchronous, pure, and easy to unit-test.
 *
 * Architecture position
 * ─────────────────────
 *
 *   CodeReviewRepositoryImpl
 *           │
 *           │  calls on Dispatchers.Default
 *           ▼
 *   KotlinCodeAnalyser.analyse(code)
 *           │
 *           ├──▶ IssueRules.*        (returns List<CodeIssue>)
 *           └──▶ OptimizationRules.* (returns List<Optimization>)
 *
 * Adding a new rule
 * ─────────────────
 * 1. Add a function to [IssueRules] or [OptimizationRules].
 * 2. Add one line here in [analyse] calling that function.
 * No other files need to change.
 */
class KotlinCodeAnalyser {

    /**
     * Runs all registered rules against [code] and returns a [ReviewResult].
     *
     * @param code Raw Kotlin source text from the editor.
     * @return     A [ReviewResult] with all detected issues and optimisations,
     *             plus a human-readable summary line.
     */
    fun analyse(code: String): ReviewResult {
        if (code.isBlank()) {
            return ReviewResult(summary = "Nothing to analyse — the editor is empty.")
        }

        // ── Run issue rules ───────────────────────────────────────────────────
        val issues = buildList {
            addAll(IssueRules.checkForceUnwrap(code))
            addAll(IssueRules.checkPrintln(code))
            addAll(IssueRules.checkEmptyCatch(code))
            addAll(IssueRules.checkMutableExposure(code))
            addAll(IssueRules.checkUnresolvedTodos(code))
            addAll(IssueRules.checkUnnecessaryVar(code))
        }

        // ── Run optimisation rules ────────────────────────────────────────────
        val optimizations = buildList {
            addAll(OptimizationRules.checkFilterMapChain(code))
            addAll(OptimizationRules.checkStringConcatInLoop(code))
            addAll(OptimizationRules.checkObjectInLoop(code))
            addAll(OptimizationRules.checkSizeGreaterThanZero(code))
            addAll(OptimizationRules.checkSizeEqualsZero(code))
            addAll(OptimizationRules.checkIfElseChain(code))
            addAll(OptimizationRules.checkManualEqualsHashCode(code))
            addAll(OptimizationRules.checkThreadSleep(code))
        }

        // ── Build summary line ────────────────────────────────────────────────
        val summary = buildSummary(issues, optimizations)

        return ReviewResult(
            issues        = issues,
            optimizations = optimizations,
            summary       = summary
        )
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildSummary(
        issues: List<com.naveedali.codereview.model.CodeIssue>,
        optimizations: List<com.naveedali.codereview.model.Optimization>
    ): String {
        if (issues.isEmpty() && optimizations.isEmpty()) {
            return "No issues found — code looks clean!"
        }

        val errorCount   = issues.count { it.severity == IssueSeverity.ERROR }
        val warningCount = issues.count { it.severity == IssueSeverity.WARNING }
        val infoCount    = issues.count { it.severity == IssueSeverity.INFO }

        return buildString {
            if (errorCount > 0)        append("${errorCount}E  ")
            if (warningCount > 0)      append("${warningCount}W  ")
            if (infoCount > 0)         append("${infoCount}I  ")
            if (optimizations.isNotEmpty()) {
                if (isNotEmpty()) append("·  ")
                append("${optimizations.size} optimisation${if (optimizations.size > 1) "s" else ""}")
            }
        }.trim()
    }
}
