package com.naveedali.claudecodereview.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Adaptive two-pane layout used by [CodeReviewScreen].
 *
 * Behaviour
 * ─────────
 *  • Portrait (maxWidth < 600 dp) — panes stack vertically, each taking 50 %
 *    of the available height. This maps naturally to "editor on top, results
 *    below" on a phone.
 *
 *  • Landscape / tablet (maxWidth ≥ 600 dp) — panes sit side by side, each
 *    taking 50 % of the available width. This gives the classic IDE split-view
 *    feel on wider screens.
 *
 * A 1 dp divider in the theme outline colour separates the two panes.
 *
 * @param startPane  Composable for the left (landscape) / top (portrait) pane.
 *                   On this screen that's the [CodeEditorPanel].
 * @param endPane    Composable for the right (landscape) / bottom (portrait) pane.
 *                   On this screen that's the [ResultsPanel].
 * @param modifier   Standard Compose modifier applied to the root container.
 */
@Composable
fun SplitPaneLayout(
    startPane: @Composable BoxScope.() -> Unit,
    endPane:   @Composable BoxScope.() -> Unit,
    modifier:  Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideLayout = maxWidth >= 600.dp

        if (isWideLayout) {
            // ── Side-by-side (landscape / tablet) ────────────────────────────
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    content  = startPane
                )

                VerticalDivider()

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    content  = endPane
                )
            }
        } else {
            // ── Stacked (portrait / phone) ────────────────────────────────────
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    content  = startPane
                )

                HorizontalDivider()

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    content  = endPane
                )
            }
        }
    }
}

// ── Divider helpers ───────────────────────────────────────────────────────────

/** 1 dp vertical rule used in wide (landscape) layout. */
@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outline)
    )
}

/** 1 dp horizontal rule used in portrait layout. */
@Composable
private fun HorizontalDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline)
    )
}
