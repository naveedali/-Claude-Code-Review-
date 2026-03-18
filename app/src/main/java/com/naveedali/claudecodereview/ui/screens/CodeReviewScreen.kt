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
 * State ownership (Phase 2)
 * ──────────────────────────
 *  • [codeInput]  — local Compose state (editor text, ephemeral, UI-only)
 *  • [uiState]    — collected from [CodeReviewViewModel] (survives rotation)
 *
 * Why split ownership?
 *  • Editor text is pure UI — no reason to round-trip through the ViewModel.
 *    It resets on back-navigation, which is the expected behaviour.
 *  • Review results are business output — they should survive configuration
 *    changes (rotation). The ViewModel outlives the Composable.
 *
 * Data flow
 * ─────────
 *
 *   User types   ──▶  codeInput (local state)
 *   "Review Code" ──▶  viewModel.review(codeInput)
 *                              │
 *                      Loading ──▶ Success / Error
 *                              │
 *                      uiState (StateFlow) collected here
 *                              │
 *                      isLoading + reviewResult derived ──▶ child panels
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

    // ── Snackbar ──────────────────────────────────────────────────────────────
    val snackbarHostState = remember { SnackbarHostState() }

    // LaunchedEffect re-runs whenever uiState changes.  When the new state is
    // Error, we show the message then ask the ViewModel to clear it — preventing
    // the same Snackbar from re-appearing on the next recomposition.
    LaunchedEffect(uiState) {
        if (uiState is ReviewUiState.Error) {
            snackbarHostState.showSnackbar(
                message  = (uiState as ReviewUiState.Error).message,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
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
