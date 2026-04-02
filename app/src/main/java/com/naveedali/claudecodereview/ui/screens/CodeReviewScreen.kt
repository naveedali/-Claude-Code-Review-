package com.naveedali.claudecodereview.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.naveedali.claudecodereview.data.preferences.LlmPreferences
import com.naveedali.claudecodereview.domain.model.ReviewUiState
import com.naveedali.claudecodereview.model.ReviewResult
import com.naveedali.claudecodereview.presentation.CodeReviewViewModel
import com.naveedali.claudecodereview.presentation.CodeReviewViewModelFactory
import com.naveedali.claudecodereview.ui.components.CodeEditorPanel
import com.naveedali.claudecodereview.ui.components.ResultsPanel
import com.naveedali.claudecodereview.ui.components.SplitPaneLayout
import com.naveedali.claudecodereview.ui.theme.ClaudeCodeReviewTheme

/**
 * Root screen composable for the code-review tool.
 *
 * Phase 3 data flow
 * ─────────────────
 *   User taps "Review Code"
 *         │
 *   viewModel.review(codeInput) → active LLM (ChatGPT / Claude / Gemini)
 *         │
 *   ReviewUiState.Success(result)
 *         │
 *   result.refactoredCode != null  ──▶  pendingRefactoredCode = refactoredCode
 *         │                                    │
 *   issues + optimizations shown        RefactorDialog appears
 *                                              │
 *                              ┌──────────────┴──────────────┐
 *                              │ "Apply"                      │ "Dismiss"
 *                              ▼                              ▼
 *                    codeInput = refactoredCode        dialog closed
 *
 * @param isDarkTheme    Current theme mode, owned by MainActivity.
 * @param onToggleTheme  Called when the user taps the sun/moon icon.
 * @param onOpenSettings Called when the user taps the settings gear icon.
 * @param viewModel      Provided via [viewModel] + [CodeReviewViewModelFactory].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeReviewScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: CodeReviewViewModel = run {
        // LocalContext.current is required because the factory reads SharedPreferences
        // to determine which LLM provider and API key to use.
        val context = LocalContext.current
        viewModel(factory = CodeReviewViewModelFactory(context))
    }
) {
    // ── Local UI state ────────────────────────────────────────────────────────
    var codeInput by remember { mutableStateOf("") }

    // ── ViewModel state ───────────────────────────────────────────────────────
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading    = uiState is ReviewUiState.Loading
    val reviewResult = (uiState as? ReviewUiState.Success)?.result ?: ReviewResult()

    // ── Refactored code dialog state ──────────────────────────────────────────
    // Copied into a local variable on first arrival; cleared on confirm/dismiss.
    // This ensures the dialog shows exactly once per new result.
    var pendingRefactoredCode by remember { mutableStateOf<String?>(null) }

    // ── Snackbar ──────────────────────────────────────────────────────────────
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is ReviewUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message  = state.message,
                    duration = SnackbarDuration.Long
                )
                viewModel.clearError()
            }
            is ReviewUiState.Success -> {
                val refactored = state.result.refactoredCode
                if (refactored != null && pendingRefactoredCode == null) {
                    pendingRefactoredCode = refactored
                }
            }
            else -> Unit
        }
    }

    // ── "Apply Refactored Code?" dialog ───────────────────────────────────────
    if (pendingRefactoredCode != null) {
        RefactorApplyDialog(
            onApply = {
                codeInput = pendingRefactoredCode!!
                pendingRefactoredCode = null
            },
            onDismiss = {
                pendingRefactoredCode = null
            }
        )
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CodeReviewTopBar(
                isDarkTheme    = isDarkTheme,
                onToggleTheme  = onToggleTheme,
                onOpenSettings = onOpenSettings
            )
        }
    ) { innerPadding ->
        SplitPaneLayout(
            modifier  = Modifier.padding(innerPadding),
            startPane = {
                CodeEditorPanel(
                    code          = codeInput,
                    onCodeChange  = { codeInput = it },
                    onReviewClick = { viewModel.review(codeInput) },
                    isLoading     = isLoading,
                    isDark        = isDarkTheme,
                    modifier      = Modifier.fillMaxSize()
                )
            },
            endPane = {
                ResultsPanel(
                    result          = reviewResult,
                    onRefactorClick = { viewModel.refactor(codeInput) },
                    isDark          = isDarkTheme,
                    modifier        = Modifier.fillMaxSize()
                )
            }
        )
    }
}

// ── "Apply Refactored Code?" dialog ───────────────────────────────────────────

/**
 * Confirmation dialog shown when the AI returns a refactored code version.
 *
 * Applying is destructive (overwrites the editor) so a confirmation step is
 * required — following the Material Design principle of preventing errors on
 * irreversible actions.
 */
@Composable
private fun RefactorApplyDialog(
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply Refactored Code?", style = MaterialTheme.typography.titleMedium) },
        text  = {
            Text(
                "The AI has produced a refactored version of your code. " +
                "Apply it to the editor? Your original code will be replaced.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton  = { Button(onClick = onApply)          { Text("Apply")         } },
        dismissButton  = { OutlinedButton(onClick = onDismiss) { Text("Keep Original") } }
    )
}

// ── Top AppBar ────────────────────────────────────────────────────────────────

/**
 * Top bar with the active provider badge, theme toggle, and settings gear.
 *
 * The active provider name is read from [LlmPreferences] and shown as a small
 * text badge in the title row — gives the user a quick reminder of which AI
 * is currently active without opening Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodeReviewTopBar(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit
) {
    // Read the active provider name for the badge — no ViewModel needed,
    // this is a pure display hint read directly from SharedPreferences.
    val context          = LocalContext.current
    val activeProvider   = remember { LlmPreferences(context).selectedProvider.displayName }

    TopAppBar(
        title = {
            Column {
                Text("Code Review", style = MaterialTheme.typography.titleLarge)
                Text(
                    text  = activeProvider,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        actions = {
            // Theme toggle
            IconButton(onClick = onToggleTheme) {
                Icon(
                    imageVector        = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (isDarkTheme) "Switch to light mode" else "Switch to dark mode",
                    tint               = MaterialTheme.colorScheme.onSurface
                )
            }
            // Settings
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector        = Icons.Default.Settings,
                    contentDescription = "Open settings",
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

@Preview(showBackground = true, name = "TopBar – Dark", widthDp = 400)
@Composable
private fun TopBarDarkPreview() {
    ClaudeCodeReviewTheme(darkTheme = true) {
        CodeReviewTopBar(isDarkTheme = true, onToggleTheme = {}, onOpenSettings = {})
    }
}

@Preview(showBackground = true, name = "TopBar – Light", widthDp = 400)
@Composable
private fun TopBarLightPreview() {
    ClaudeCodeReviewTheme(darkTheme = false) {
        CodeReviewTopBar(isDarkTheme = false, onToggleTheme = {}, onOpenSettings = {})
    }
}

@Preview(showBackground = true, name = "Refactor Dialog", widthDp = 400)
@Composable
private fun RefactorDialogPreview() {
    ClaudeCodeReviewTheme(darkTheme = false) {
        RefactorApplyDialog(onApply = {}, onDismiss = {})
    }
}
