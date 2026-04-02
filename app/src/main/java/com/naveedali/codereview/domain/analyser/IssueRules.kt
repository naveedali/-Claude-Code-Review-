package com.naveedali.codereview.domain.analyser

import com.naveedali.codereview.model.CodeIssue
import com.naveedali.codereview.model.IssueSeverity

/**
 * A collection of pure functions that scan Kotlin source text for code issues.
 *
 * Design decisions
 * ────────────────
 * • Each rule is a standalone function returning a List<CodeIssue>. This makes
 *   individual rules independently testable (Phase 2 tests can call a single rule).
 * • Rules operate on the raw String — no AST, no compiler.  This means they can
 *   produce false positives (e.g. patterns inside string literals) but are fast,
 *   zero-dependency, and already catch the most common real-world problems.
 * • All IDs are deterministic (rule name + line number) so LazyColumn keys stay
 *   stable across re-runs of the same code.
 */
internal object IssueRules {

    // ── Public API ────────────────────────────────────────────────────────────

    /** Detects the `!!` (non-null assertion) operator which crashes on null. */
    fun checkForceUnwrap(code: String): List<CodeIssue> = buildList {
        code.lines().forEachIndexed { index, line ->
            if (isCodeLine(line) && line.contains("!!")) {
                add(
                    CodeIssue(
                        id          = "force_unwrap_${index + 1}",
                        severity    = IssueSeverity.ERROR,
                        lineNumber  = index + 1,
                        title       = "Force unwrap (!!) detected",
                        description = "The !! operator asserts non-null at runtime. " +
                                      "If the value is null it throws NullPointerException — " +
                                      "one of the most common crash sources in Kotlin.",
                        suggestion  = "Replace with a safe-call + Elvis: `value?.doSomething() ?: fallback`"
                    )
                )
            }
        }
    }

    /**
     * Detects `println()` calls, which write to stdout (invisible on Android)
     * and can leak data in production logs.
     */
    fun checkPrintln(code: String): List<CodeIssue> = buildList {
        val regex = Regex("""\bprintln\s*\(""")
        code.lines().forEachIndexed { index, line ->
            if (isCodeLine(line) && regex.containsMatchIn(line)) {
                add(
                    CodeIssue(
                        id          = "println_${index + 1}",
                        severity    = IssueSeverity.WARNING,
                        lineNumber  = index + 1,
                        title       = "println() in production code",
                        description = "println() writes to stdout, which is swallowed on Android and " +
                                      "can expose sensitive data in production environments.",
                        suggestion  = "Use Log.d(TAG, message) or Timber.d(message) instead."
                    )
                )
            }
        }
    }

    /**
     * Detects catch blocks with empty or comment-only bodies, which silently
     * swallow exceptions.
     *
     * Pattern matched: `catch (e: SomeType) { }` or `catch (e: SomeType) { // comment }`
     */
    fun checkEmptyCatch(code: String): List<CodeIssue> = buildList {
        val regex = Regex(
            pattern = """catch\s*\(\s*\w+(?:\s*:\s*[\w.]+)?\s*\)\s*\{\s*(//[^\n]*)?\s*\}""",
            options = setOf(RegexOption.MULTILINE)
        )
        regex.findAll(code).forEach { match ->
            val lineNumber = code.substring(0, match.range.first).lines().size
            add(
                CodeIssue(
                    id          = "empty_catch_$lineNumber",
                    severity    = IssueSeverity.WARNING,
                    lineNumber  = lineNumber,
                    title       = "Empty catch block",
                    description = "Silently swallowing exceptions hides bugs and makes debugging " +
                                  "extremely difficult — especially in production.",
                    suggestion  = "At minimum log the exception: " +
                                  "`catch (e: Exception) { Log.e(TAG, \"Failed\", e) }`"
                )
            )
        }
    }

    /**
     * Detects public functions whose return type is a mutable collection
     * (MutableList, MutableMap, MutableSet, MutableCollection), which exposes
     * internal state to callers.
     */
    fun checkMutableExposure(code: String): List<CodeIssue> = buildList {
        val regex = Regex("""fun\s+\w+[^{(]*\(\s*\)\s*:\s*Mutable(List|Map|Set|Collection)""")
        code.lines().forEachIndexed { index, line ->
            if (isCodeLine(line) && regex.containsMatchIn(line)) {
                add(
                    CodeIssue(
                        id          = "mutable_exposure_${index + 1}",
                        severity    = IssueSeverity.WARNING,
                        lineNumber  = index + 1,
                        title       = "Mutable collection returned from function",
                        description = "Returning a mutable collection lets any caller modify internal " +
                                      "state directly, breaking encapsulation and making state management " +
                                      "unpredictable.",
                        suggestion  = "Return the immutable interface: " +
                                      "`List<T>`, `Map<K, V>`, or `Set<T>`."
                    )
                )
            }
        }
    }

    /**
     * Flags TODO, FIXME, HACK, and XXX markers left in code.
     * These should be tracked in an issue tracker, not as inline comments.
     */
    fun checkUnresolvedTodos(code: String): List<CodeIssue> = buildList {
        val regex = Regex("""\b(TODO|FIXME|HACK|XXX)\b""", RegexOption.IGNORE_CASE)
        code.lines().forEachIndexed { index, line ->
            if (regex.containsMatchIn(line)) {
                val keyword = regex.find(line)?.value?.uppercase() ?: "TODO"
                add(
                    CodeIssue(
                        id          = "todo_${index + 1}",
                        severity    = IssueSeverity.INFO,
                        lineNumber  = index + 1,
                        title       = "Unresolved $keyword marker",
                        description = "An inline $keyword comment was found. These are often forgotten " +
                                      "and should be tracked in your project's issue tracker.",
                        suggestion  = "Create a tracked issue, then remove the inline marker."
                    )
                )
            }
        }
    }

    /**
     * Detects variables declared with `var` that are never reassigned after
     * their initial declaration (simple single-line heuristic).
     *
     * This catches the most common case: `var x = value` where x only appears
     * once more (reading). For complex multi-line reassignment this rule will
     * not trigger (acceptable false-negative for a regex-based approach).
     */
    fun checkUnnecessaryVar(code: String): List<CodeIssue> = buildList {
        val varRegex = Regex("""^\s*var\s+(\w+)\s*=""")
        code.lines().forEachIndexed { lineIndex, line ->
            val match = varRegex.find(line) ?: return@forEachIndexed
            val varName = match.groupValues[1]

            // Check how many times the name appears in the rest of the code
            val remainingCode = code.lines().drop(lineIndex + 1).joinToString("\n")
            val assignmentPattern = Regex("""\b${Regex.escape(varName)}\s*=""")

            // If there's no reassignment after the declaration, suggest val
            if (!assignmentPattern.containsMatchIn(remainingCode)) {
                add(
                    CodeIssue(
                        id          = "unnecessary_var_${lineIndex + 1}",
                        severity    = IssueSeverity.WARNING,
                        lineNumber  = lineIndex + 1,
                        title       = "var can be val: `$varName`",
                        description = "`$varName` is declared with `var` but never reassigned. " +
                                      "Prefer `val` (immutable) unless mutation is truly needed — " +
                                      "it communicates intent and prevents accidental reassignment.",
                        suggestion  = "Replace `var $varName` with `val $varName`."
                    )
                )
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Returns `true` if [line] is a real code line (not blank, not a comment).
     * Skips `//` line comments and KDoc `*` lines.
     */
    private fun isCodeLine(line: String): Boolean {
        val trimmed = line.trimStart()
        return trimmed.isNotBlank()
                && !trimmed.startsWith("//")
                && !trimmed.startsWith("*")
    }
}
