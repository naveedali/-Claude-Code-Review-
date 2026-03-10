package com.naveedali.claudecodereview.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.naveedali.claudecodereview.model.ReviewResult
import com.naveedali.claudecodereview.model.mockReviewResult
import com.naveedali.claudecodereview.ui.components.CodeEditorPanel
import com.naveedali.claudecodereview.ui.components.ResultsPanel
import com.naveedali.claudecodereview.ui.components.SplitPaneLayout
import com.naveedali.claudecodereview.ui.theme.ClaudeCodeReviewTheme

/**
 * Root screen composable for the code-review tool.
 *
 * State owned here (Phase 1 — UI only)
 * ─────────────────────────────────────
 *  • [codeInput]     — text currently in the editor
 *  • [reviewResult]  — result displayed in the results panel (mock data for now)
 *  • [isLoading]     — drives the loading state of the Review button
 *  • [isDarkTheme]   — toggles the app-wide theme; exposed to [MainActivity]
 *
 * Phase 2 will hoist [reviewResult] and [isLoading] into a ViewModel and wire
 * them to an AI service call triggered by [onReviewClick].
 *
 * @param isDarkTheme      Current theme mode.
 * @param onToggleTheme    Called when the user taps the theme toggle icon in the
 *                         top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeReviewScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    // ── Local UI state ────────────────────────────────────────────────────────
    var codeInput    by remember { mutableStateOf("") }
    var reviewResult by remember { mutableStateOf<ReviewResult?>(null) }
    var isLoading    by remember { mutableStateOf(false) }

    // ── Callbacks ─────────────────────────────────────────────────────────────

    /**
     * Called when the user presses "Review Code".
     *
     * Phase 1: immediately shows mock results (no delay / network call).
     * Phase 2: will trigger a suspend function in a ViewModel.
     */
    val onReviewClick: () -> Unit = {
        // TODO Phase 2 — replace with viewModel.review(codeInput)
        reviewResult = mockReviewResult
    }

    /**
     * Called when the user presses "Refactor Code".
     *
     * Phase 1: no-op — the button is visible but does nothing yet.
     * Phase 2: will request the refactored snippet from the AI.
     */
    val onRefactorClick: () -> Unit = {
        // TODO Phase 2 — viewModel.refactor(codeInput)
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            CodeReviewTopBar(
                isDarkTheme   = isDarkTheme,
                onToggleTheme = onToggleTheme
            )
        }
    ) { innerPadding ->
        SplitPaneLayout(
            modifier  = Modifier.padding(innerPadding),
            startPane = {
                CodeEditorPanel(
                    code          = codeInput,
                    onCodeChange  = { codeInput = it },
                    onReviewClick = onReviewClick,
                    isLoading     = isLoading,
                    modifier      = Modifier.fillMaxSize()
                )
            },
            endPane = {
                ResultsPanel(
                    result          = reviewResult ?: ReviewResult(),
                    onRefactorClick = onRefactorClick,
                    isDark          = isDarkTheme,
                    modifier        = Modifier.fillMaxSize()
                )
            }
        )
    }
}

// ── Top AppBar ────────────────────────────────────────────────────────────────

/**
 * Slim top bar with the app title and a light/dark toggle icon.
 *
 * Keeping the top bar in its own composable makes it easy to swap in a
 * navigation icon or add a menu without touching [CodeReviewScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodeReviewTopBar(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text  = "Code Review",
                style = MaterialTheme.typography.titleLarge
            )
        },
        actions = {
            IconButton(onClick = onToggleTheme) {
                Icon(
                    imageVector        = if (isDarkTheme) Icons.Default.LightMode
                                         else             Icons.Default.DarkMode,
                    contentDescription = if (isDarkTheme) "Switch to light mode"
                                         else             "Switch to dark mode",
                    tint               = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Screen – Dark", widthDp = 400, heightDp = 800)
@Composable
private fun CodeReviewScreenDarkPreview() {
    ClaudeCodeReviewTheme(darkTheme = true) {
        CodeReviewScreen(isDarkTheme = true, onToggleTheme = {})
    }
}

@Preview(showBackground = true, name = "Screen – Light", widthDp = 400, heightDp = 800)
@Composable
private fun CodeReviewScreenLightPreview() {
    ClaudeCodeReviewTheme(darkTheme = false) {
        CodeReviewScreen(isDarkTheme = false, onToggleTheme = {})
    }
}

@Preview(
    showBackground = true,
    name            = "Screen – Landscape / Tablet",
    widthDp         = 840,
    heightDp        = 420
)
@Composable
private fun CodeReviewScreenLandscapePreview() {
    ClaudeCodeReviewTheme(darkTheme = true) {
        CodeReviewScreen(isDarkTheme = true, onToggleTheme = {})
    }
}
