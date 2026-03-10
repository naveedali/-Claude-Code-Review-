package com.naveedali.claudecodereview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import com.naveedali.claudecodereview.ui.screens.CodeReviewScreen
import com.naveedali.claudecodereview.ui.theme.ClaudeCodeReviewTheme

/**
 * Single-activity entry point.
 *
 * Theme state is owned here so it survives recomposition and can be toggled
 * from anywhere in the tree if needed.  [isSystemInDarkTheme] provides the
 * default so the very first launch respects the device setting.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Read the system preference once as the initial value
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDark) }

            ClaudeCodeReviewTheme(darkTheme = isDarkTheme) {
                CodeReviewScreen(
                    isDarkTheme   = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }
}
