package com.naveedali.codereview.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.naveedali.codereview.data.preferences.LlmPreferences
import com.naveedali.codereview.domain.model.ReviewUiState
import com.naveedali.codereview.model.ReviewResult
import com.naveedali.codereview.presentation.CodeReviewViewModel
import com.naveedali.codereview.presentation.CodeReviewViewModelFactory
import com.naveedali.codereview.ui.components.CodeEditorPanel
import com.naveedali.codereview.ui.components.ResultsPanel
import com.naveedali.codereview.ui.components.SplitPaneLayout
import com.naveedali.codereview.ui.theme.ClaudeCodeReviewTheme

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
        val ctx = LocalContext.current
        viewModel(factory = CodeReviewViewModelFactory(ctx))
    }
) {
    // ── Context + preferences (for API-key gate check) ────────────────────────
    val context        = LocalContext.current
    val llmPreferences = remember { LlmPreferences(context) }

    // ── Local UI state ────────────────────────────────────────────────────────
    var codeInput       by remember { mutableStateOf("") }
    var showNoKeyDialog by remember { mutableStateOf(false) }

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

    // ── "No API key" dialog ───────────────────────────────────────────────────
    if (showNoKeyDialog) {
        NoApiKeyDialog(
            providerName   = llmPreferences.selectedProvider.displayName,
            onAddKey       = { showNoKeyDialog = false; onOpenSettings() },
            onDismiss      = { showNoKeyDialog = false }
        )
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
                    onReviewClick = {
                        if (llmPreferences.activeApiKey().isBlank()) {
                            showNoKeyDialog = true
                        } else {
                            viewModel.review(codeInput)
                        }
                    },
                    isLoading     = isLoading,
                    isDark        = isDarkTheme,
                    modifier      = Modifier.fillMaxSize()
                )
            },
            endPane = {
                ResultsPanel(
                    result          = reviewResult,
                    onRefactorClick = {
                        if (llmPreferences.activeApiKey().isBlank()) {
                            showNoKeyDialog = true
                        } else {
                            viewModel.refactor(codeInput)
                        }
                    },
                    isDark          = isDarkTheme,
                    modifier        = Modifier.fillMaxSize()
                )
            }
        )
    }
}

// ── "No API key configured" dialog ────────────────────────────────────────────

/**
 * Shown when the user taps "Review Code" or "Refactor" but the active provider
 * has no API key stored in Settings.
 *
 * "Add Key" navigates straight to the Settings screen so the user can enter
 * their key without hunting for the gear icon.
 *
 * @param providerName  Display name of the currently selected provider
 *                      (e.g. "ChatGPT", "Claude", "Gemini").
 * @param onAddKey      Called when the user taps "Add Key" — navigate to Settings.
 * @param onDismiss     Called when the user taps "Not Now" or back.
 */
@Composable
private fun NoApiKeyDialog(
    providerName: String,
    onAddKey:  () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector        = Icons.Default.Key,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text       = "API Key Required",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text  = "No API key is configured for $providerName. " +
                        "Add your key in Settings to enable AI-powered code review.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onAddKey) {
                Icon(
                    imageVector        = Icons.Default.Settings,
                    contentDescription = null,
                    modifier           = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Add Key")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Not Now") }
        }
    )
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
