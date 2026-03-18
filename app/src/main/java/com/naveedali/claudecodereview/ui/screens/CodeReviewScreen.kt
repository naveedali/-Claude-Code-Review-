package com.naveedali.claudecodereview.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
 * State ownership (Phase 3)
 * ──────────────────────────
 *  • [codeInput]              — local Compose state (editor text, ephemeral, UI-only)
 *  • [uiState]                — collected from [CodeReviewViewModel] (survives rotation)
 *  • [pendingRefactoredCode]  — local flag; set when a new refactored result arrives,
 *                               cleared when the dialog is confirmed or dismissed.
 *
 * Phase 3 data flow
 * ─────────────────
 *
 *   User taps "Review Code"
 *         │
 *   viewModel.review(codeInput) → OpenAI GPT-4o
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
 * @param isDarkTheme   Current theme mode, owned by MainActivity.
 * @param onToggleTheme Called when the user taps the sun/moon icon.
 * @param viewModel     Provided via [viewModel] + [CodeReviewViewModelFactory].
 *                      Can be overridden in tests with a fake ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeReviewScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    viewModel: CodeReviewViewModel = viewModel(factory = CodeReviewViewModelFactory())
) {
    // ── Local UI state (editor text only) ────────────────────────────────────
    var codeInput by remember { mutableStateOf("") }

    // ── ViewModel state ───────────────────────────────────────────────────────
    // collectAsStateWithLifecycle pauses collection when the screen is in the
    // background — saving battery compared to plain collectAsState().
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Derived values — child composables see only the primitives they need,
    // not the sealed class itself.  This keeps the pane composables decoupled.
    val isLoading    = uiState is ReviewUiState.Loading
    val reviewResult = (uiState as? ReviewUiState.Success)?.result ?: ReviewResult()

    // ── Phase 3: Refactored code dialog state ─────────────────────────────────
    //
    // Why a separate local variable instead of reading reviewResult.refactoredCode
    // directly?
    //
    // Reading refactoredCode directly would show the dialog on every recomposition
    // once a result with refactoredCode arrives — including after the user dismisses
    // it.  By copying the value into [pendingRefactoredCode] and clearing it on
    // dismiss/confirm, the dialog appears exactly once per new refactor result.
    var pendingRefactoredCode by remember { mutableStateOf<String?>(null) }

    // ── Snackbar ──────────────────────────────────────────────────────────────
    val snackbarHostState = remember { SnackbarHostState() }

    // LaunchedEffect re-runs whenever uiState changes.
    // Handles two transitions:
    //  1. Error  → show Snackbar, then ask ViewModel to clear the error state.
    //  2. Success with refactoredCode → set pendingRefactoredCode to trigger dialog.
    //
    // Using uiState as the key means the effect re-runs only when the state
    // object changes identity — not on every recomposition.
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
                // If the AI returned a refactored version, surface the dialog.
                // We only trigger when the refactoredCode is non-null and not yet
                // pending — prevents re-triggering if the user is still looking
                // at the dialog when a recomposition happens.
                val refactored = state.result.refactoredCode
                if (refactored != null && pendingRefactoredCode == null) {
                    pendingRefactoredCode = refactored
                }
            }
            else -> Unit  // Idle / Loading — no side effects needed
        }
    }

    // ── "Apply Refactored Code?" dialog ───────────────────────────────────────
    //
    // Shown whenever [pendingRefactoredCode] is non-null (i.e. a new refactored
    // version just arrived from the AI).
    //
    // Choices:
    //  • "Apply"   — copies the refactored code into the editor, closes dialog
    //  • "Dismiss" — closes dialog without changing the editor content
    if (pendingRefactoredCode != null) {
        RefactorApplyDialog(
            onApply = {
                codeInput = pendingRefactoredCode!!   // replace editor content
                pendingRefactoredCode = null          // close dialog
            },
            onDismiss = {
                pendingRefactoredCode = null          // close dialog, keep old code
            }
        )
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
 * Material 3 [AlertDialog] that asks the user whether to replace the editor
 * content with the AI-generated refactored code.
 *
 * Why a dialog instead of an inline button in the results panel?
 * ──────────────────────────────────────────────────────────────
 * Applying the refactored code is a destructive action — it overwrites the
 * user's current input.  A confirmation dialog follows the Material Design
 * principle of "preventing errors by requiring confirmation for irreversible
 * or significant actions."
 *
 * The dialog is stateless — it just fires [onApply] or [onDismiss] and the
 * caller owns the state.  This makes it easy to test and reuse.
 *
 * @param onApply   Called when the user confirms ("Apply").
 * @param onDismiss Called when the user cancels ("Keep Original") or dismisses.
 */
@Composable
private fun RefactorApplyDialog(
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text  = "Apply Refactored Code?",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Text(
                text  = "The AI has produced a refactored version of your code. " +
                        "Apply it to the editor? Your original code will be replaced.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            // Filled button — the primary / recommended action
            Button(onClick = onApply) {
                Text("Apply")
            }
        },
        dismissButton = {
            // Outlined button — the secondary / safe action
            OutlinedButton(onClick = onDismiss) {
                Text("Keep Original")
            }
        }
    )
}

// ── Top AppBar ────────────────────────────────────────────────────────────────

/**
 * Slim top bar with the app title and a light/dark toggle.
 *
 * Extracted into its own composable so navigation icons or an overflow menu
 * can be added here later without touching [CodeReviewScreen].
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
//
// The full screen needs a real ViewModelStoreOwner which isn't available in
// the preview environment.  We preview the top bar (stable, no ViewModel) and
// trust the individual pane previews for the content areas.

@Preview(showBackground = true, name = "TopBar – Dark", widthDp = 400)
@Composable
private fun TopBarDarkPreview() {
    ClaudeCodeReviewTheme(darkTheme = true) {
        CodeReviewTopBar(isDarkTheme = true, onToggleTheme = {})
    }
}

@Preview(showBackground = true, name = "TopBar – Light", widthDp = 400)
@Composable
private fun TopBarLightPreview() {
    ClaudeCodeReviewTheme(darkTheme = false) {
        CodeReviewTopBar(isDarkTheme = false, onToggleTheme = {})
    }
}

@Preview(showBackground = true, name = "Refactor Dialog", widthDp = 400)
@Composable
private fun RefactorDialogPreview() {
    ClaudeCodeReviewTheme(darkTheme = false) {
        RefactorApplyDialog(onApply = {}, onDismiss = {})
    }
}
