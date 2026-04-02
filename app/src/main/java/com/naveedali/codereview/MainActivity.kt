package com.naveedali.codereview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import com.naveedali.codereview.ui.screens.CodeReviewScreen
import com.naveedali.codereview.ui.screens.SettingsScreen
import com.naveedali.codereview.ui.theme.ClaudeCodeReviewTheme

/**
 * The two top-level destinations in the app.
 *
 * We don't use the Navigation component here — with only two screens a simple
 * enum + `when` expression is lighter and easier to understand.  If more screens
 * are added, replacing this with NavHost is straightforward.
 */
enum class Screen { CODE_REVIEW, SETTINGS }

/**
 * Single-activity entry point.
 *
 * State owned here:
 *  • [isDarkTheme]       — survives recomposition, toggled from the top bar.
 *  • [currentScreen]     — controls which screen is shown.
 *  • [reviewScreenKey]   — incremented when the user saves Settings, which forces
 *                          Compose to discard and recreate [CodeReviewScreen]
 *                          (and its ViewModel) so the new provider is picked up
 *                          immediately without restarting the app.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme     by remember { mutableStateOf(systemDark) }
            var currentScreen   by remember { mutableStateOf(Screen.CODE_REVIEW) }
            var reviewScreenKey by remember { mutableIntStateOf(0) }

            ClaudeCodeReviewTheme(darkTheme = isDarkTheme) {
                when (currentScreen) {
                    Screen.CODE_REVIEW -> {
                        // key() forces a full recomposition of CodeReviewScreen —
                        // and thus a new ViewModel — whenever reviewScreenKey changes.
                        // This is the idiomatic Compose way to "reset" a subtree.
                        key(reviewScreenKey) {
                            CodeReviewScreen(
                                isDarkTheme    = isDarkTheme,
                                onToggleTheme  = { isDarkTheme = !isDarkTheme },
                                onOpenSettings = { currentScreen = Screen.SETTINGS }
                            )
                        }
                    }
                    Screen.SETTINGS -> SettingsScreen(
                        isDarkTheme = isDarkTheme,
                        onBack = {
                            // Bump key so the ViewModel is recreated with the new provider
                            reviewScreenKey++
                            currentScreen = Screen.CODE_REVIEW
                        }
                    )
                }
            }
        }
    }
}
