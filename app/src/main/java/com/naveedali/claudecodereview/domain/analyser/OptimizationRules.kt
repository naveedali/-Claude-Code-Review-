package com.naveedali.claudecodereview.domain.analyser

import com.naveedali.claudecodereview.model.Optimization
import com.naveedali.claudecodereview.model.OptimizationType

/**
 * A collection of pure functions that scan Kotlin source text for
 * optimisation opportunities.
 *
 * Categories (mapped to [OptimizationType] chips in the UI)
 * ──────────────────────────────────────────────────────────
 *  PERFORMANCE   — avoids unnecessary work or allocations
 *  READABILITY   — makes code intent clearer for future readers
 *  MEMORY        — reduces heap allocations / GC pressure
 *  BEST_PRACTICE — idiomatic Kotlin that leverages language features
 */
internal object OptimizationRules {

    // ── PERFORMANCE ───────────────────────────────────────────────────────────

    /**
     * Detects `.filter{}.map{}` chains on plain Lists.
     *
     * Each step creates a full intermediate List. For large collections,
     * converting to a Sequence processes elements lazily — no intermediate lists.
     */
    fun checkFilterMapChain(code: String): List<Optimization> {
        val chainRegex = Regex("""\.filter\s*\{[^}]+\}\s*\.map\s*\{""")
        return if (chainRegex.containsMatchIn(code) && !code.contains(".asSequence()")) {
            listOf(
                Optimization(
                    id          = "opt_filter_map_sequence",
                    type        = OptimizationType.PERFORMANCE,
                    title       = "Use Sequence for filter + map chain",
                    description = "Chaining .filter{} and .map{} on a plain List creates an intermediate " +
                                  "List after each step. A Sequence processes elements lazily — " +
                                  "no intermediate allocation for collections > ~100 elements.",
                    beforeCode  = "list.filter { it.active }.map { it.name }",
                    afterCode   = "list.asSequence()\n    .filter { it.active }\n    .map { it.name }\n    .toList()"
                )
            )
        } else emptyList()
    }

    /**
     * Detects String concatenation with `+=` inside a loop body.
     *
     * Each `+=` on a String creates a new String object (Strings are immutable).
     * `buildString {}` wraps a single StringBuilder for the entire loop.
     */
    fun checkStringConcatInLoop(code: String): List<Optimization> {
        val hasLoop        = Regex("""\bfor\s*\(|\bwhile\s*\(""").containsMatchIn(code)
        val hasStringVar   = Regex("""\bvar\s+\w+\s*=\s*""" + "\"").containsMatchIn(code)
        val hasPlusAssign  = Regex("""\w+\s*\+=\s*""").containsMatchIn(code)

        return if (hasLoop && hasStringVar && hasPlusAssign) {
            listOf(
                Optimization(
                    id          = "opt_build_string",
                    type        = OptimizationType.PERFORMANCE,
                    title       = "Use buildString instead of String += in loop",
                    description = "String is immutable — each `+=` allocates a new object and copies " +
                                  "the previous content. For N iterations this is O(N²) in both time " +
                                  "and memory. buildString uses a single StringBuilder internally.",
                    beforeCode  = "var result = \"\"\nfor (item in list) {\n    result += item.name\n}",
                    afterCode   = "val result = buildString {\n    for (item in list) {\n        append(item.name)\n    }\n}"
                )
            )
        } else emptyList()
    }

    // ── MEMORY ────────────────────────────────────────────────────────────────

    /**
     * Detects object allocations inside `for` loop bodies.
     *
     * Heuristic: looks for `val name = UpperCase(` directly inside a for block.
     * Reusable objects (e.g. StringBuilder, Paint) should be moved outside.
     */
    fun checkObjectInLoop(code: String): List<Optimization> {
        val regex = Regex(
            pattern = """for\s*\([^)]+\)\s*\{[^}]*\bval\s+\w+\s*=\s*[A-Z]\w*\s*\(""",
            options = setOf(RegexOption.DOT_MATCHES_ALL)
        )
        return if (regex.containsMatchIn(code)) {
            listOf(
                Optimization(
                    id          = "opt_alloc_outside_loop",
                    type        = OptimizationType.MEMORY,
                    title       = "Avoid allocating objects inside a loop",
                    description = "Creating a new object on every iteration increases garbage-collector " +
                                  "pressure proportional to the loop count. If the object is resettable " +
                                  "(e.g. StringBuilder, Paint), allocate once and clear between uses.",
                    beforeCode  = "for (item in items) {\n    val sb = StringBuilder()\n    sb.append(item.name)\n}",
                    afterCode   = "val sb = StringBuilder()\nfor (item in items) {\n    sb.clear()\n    sb.append(item.name)\n}"
                )
            )
        } else emptyList()
    }

    // ── READABILITY ───────────────────────────────────────────────────────────

    /**
     * Detects `.size > 0` which should use the idiomatic `.isNotEmpty()`.
     */
    fun checkSizeGreaterThanZero(code: String): List<Optimization> =
        if (Regex("""\.\s*size\s*>\s*0""").containsMatchIn(code)) {
            listOf(
                Optimization(
                    id          = "opt_is_not_empty",
                    type        = OptimizationType.READABILITY,
                    title       = "Replace .size > 0 with .isNotEmpty()",
                    description = ".isNotEmpty() communicates intent clearly, reads like English, " +
                                  "and is the idiomatic Kotlin way to check for a non-empty collection.",
                    beforeCode  = "if (list.size > 0) { … }",
                    afterCode   = "if (list.isNotEmpty()) { … }"
                )
            )
        } else emptyList()

    /**
     * Detects `.size == 0` which should use the idiomatic `.isEmpty()`.
     */
    fun checkSizeEqualsZero(code: String): List<Optimization> =
        if (Regex("""\.\s*size\s*==\s*0""").containsMatchIn(code)) {
            listOf(
                Optimization(
                    id          = "opt_is_empty",
                    type        = OptimizationType.READABILITY,
                    title       = "Replace .size == 0 with .isEmpty()",
                    description = ".isEmpty() is the idiomatic Kotlin extension for checking empty " +
                                  "collections. It is immediately understandable and avoids the " +
                                  "accidental confusion between size and index.",
                    beforeCode  = "if (list.size == 0) { … }",
                    afterCode   = "if (list.isEmpty()) { … }"
                )
            )
        } else emptyList()

    /**
     * Detects `if / else if / else if` chains with 2+ branches on the same
     * variable, which should use a `when` expression.
     */
    fun checkIfElseChain(code: String): List<Optimization> {
        val elseIfCount = Regex("""\belse\s+if\s*\(""").findAll(code).count()
        return if (elseIfCount >= 2) {
            listOf(
                Optimization(
                    id          = "opt_when_expression",
                    type        = OptimizationType.READABILITY,
                    title       = "Replace if-else chain with when",
                    description = "Multiple `else if` branches are verbose and easy to mis-order. " +
                                  "Kotlin's `when` expression is more readable, the compiler " +
                                  "can warn on missing branches, and it can be used as an expression.",
                    beforeCode  = "if (x == 1) a()\nelse if (x == 2) b()\nelse c()",
                    afterCode   = "when (x) {\n    1    -> a()\n    2    -> b()\n    else -> c()\n}"
                )
            )
        } else emptyList()
    }

    // ── BEST PRACTICE ─────────────────────────────────────────────────────────

    /**
     * Detects manual `override fun equals()` paired with `override fun hashCode()`
     * — a strong signal that a `data class` would be simpler.
     */
    fun checkManualEqualsHashCode(code: String): List<Optimization> {
        val hasEquals   = Regex("""override\s+fun\s+equals\s*\(""").containsMatchIn(code)
        val hasHashCode = Regex("""override\s+fun\s+hashCode\s*\(""").containsMatchIn(code)
        return if (hasEquals && hasHashCode) {
            listOf(
                Optimization(
                    id          = "opt_data_class",
                    type        = OptimizationType.BEST_PRACTICE,
                    title       = "Consider data class instead of manual equals/hashCode",
                    description = "This class manually implements equals() and hashCode(). A `data class` " +
                                  "auto-generates them from constructor properties, plus adds copy(), " +
                                  "componentN() destructuring, and a human-readable toString().",
                    beforeCode  = "class Point(val x: Int, val y: Int) {\n    override fun equals(other: Any?) …\n    override fun hashCode() …\n}",
                    afterCode   = "data class Point(val x: Int, val y: Int)"
                )
            )
        } else emptyList()
    }

    /**
     * Detects direct `Thread.sleep()` calls, which block the calling thread.
     * In a coroutine context `delay()` should be used instead.
     */
    fun checkThreadSleep(code: String): List<Optimization> {
        val regex = Regex("""\bThread\s*\.\s*sleep\s*\(""")
        return if (regex.containsMatchIn(code)) {
            listOf(
                Optimization(
                    id          = "opt_thread_sleep",
                    type        = OptimizationType.BEST_PRACTICE,
                    title       = "Replace Thread.sleep() with delay() in coroutines",
                    description = "Thread.sleep() blocks the OS thread, preventing it from doing other work. " +
                                  "Inside a coroutine, `delay()` suspends without blocking the thread, " +
                                  "which is far more efficient under high concurrency.",
                    beforeCode  = "Thread.sleep(1000)",
                    afterCode   = "delay(1000)   // inside a suspend fun or coroutine scope"
                )
            )
        } else emptyList()
    }
}
