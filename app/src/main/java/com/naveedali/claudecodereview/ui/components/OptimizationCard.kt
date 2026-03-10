package com.naveedali.claudecodereview.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.naveedali.claudecodereview.model.Optimization
import com.naveedali.claudecodereview.model.OptimizationType
import com.naveedali.claudecodereview.model.mockOptimizations
import com.naveedali.claudecodereview.ui.theme.*

// ── OptimizationType → label + accent colour ──────────────────────────────────

private data class OptStyle(val label: String, val color: Color)

private val optStyles = mapOf(
    OptimizationType.PERFORMANCE   to OptStyle("PERFORMANCE",  OptPerfPink),
    OptimizationType.READABILITY   to OptStyle("READABILITY",  OptReadCyan),
    OptimizationType.MEMORY        to OptStyle("MEMORY",       OptMemGreen),
    OptimizationType.BEST_PRACTICE to OptStyle("BEST PRACTICE", OptBestPurple),
)

/**
 * An expandable card for a single optimisation suggestion.
 *
 * When the card has before/after code snippets, expanding reveals a two-column
 * diff-style view so the user can see what to change at a glance.
 *
 * @param optimization The [Optimization] to render.
 * @param isDark       Dark-/light-mode toggle passed from the enclosing theme.
 * @param modifier     Standard Compose modifier.
 */
@Composable
fun OptimizationCard(
    optimization: Optimization,
    isDark: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val style = optStyles[optimization.type] ?: optStyles[OptimizationType.BEST_PRACTICE]!!
    val hasSnippets = optimization.beforeCode != null || optimization.afterCode != null

    // Subtle surface tint derived from the type accent
    val cardBg = if (isDark)
        SurfaceDark.copy(alpha = 0.8f)
    else
        SurfaceLight

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(cardBg)
            .border(
                width = 1.dp,
                color = style.color.copy(alpha = 0.35f),
                shape = RoundedCornerShape(10.dp)
            )
            .then(
                // Only make clickable if there's expandable content
                if (hasSnippets) Modifier.clickable { expanded = !expanded } else Modifier
            )
            .padding(12.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Type chip
            OptTypeBadge(label = style.label, color = style.color)

            // Title
            Text(
                text     = optimization.title,
                style    = MaterialTheme.typography.titleSmall,
                color    = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Chevron when snippets are available
            if (hasSnippets) {
                Icon(
                    imageVector        = if (expanded) Icons.Default.KeyboardArrowUp
                                         else           Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand snippets",
                    tint               = style.color,
                    modifier           = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // ── Description ───────────────────────────────────────────────────────
        Text(
            text  = optimization.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        // ── Expandable before / after snippets ────────────────────────────────
        if (hasSnippets) {
            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(
                        color     = style.color.copy(alpha = 0.25f),
                        thickness = 1.dp
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Before column
                        optimization.beforeCode?.let { before ->
                            CodeSnippetBox(
                                label   = "Before",
                                code    = before,
                                accent  = ErrorRed,
                                isDark  = isDark,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // After column
                        optimization.afterCode?.let { after ->
                            CodeSnippetBox(
                                label   = "After",
                                code    = after,
                                accent  = SuccessGreen,
                                isDark  = isDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Private helpers ───────────────────────────────────────────────────────────

/** Coloured chip showing the optimisation category. */
@Composable
private fun OptTypeBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium.copy(color = color)
        )
    }
}

/**
 * A labelled monospace code box used in the before/after diff view.
 *
 * @param label  "Before" or "After".
 * @param code   The code snippet string.
 * @param accent Tint colour for the label and border.
 * @param isDark Dark/light mode switch.
 */
@Composable
private fun CodeSnippetBox(
    label: String,
    code: String,
    accent: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val snippetBg = if (isDark) EditorBgDark else EditorBgLight

    Column(modifier = modifier) {
        // Label
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall.copy(color = accent),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        // Code block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(snippetBg)
                .border(
                    width = 1.dp,
                    color = accent.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(8.dp)
        ) {
            Text(
                text  = code,
                style = CodeSnippetTextStyle.copy(
                    color = if (isDark) CodeTextDark else CodeTextLight
                )
            )
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "OptimizationCard – Dark")
@Composable
private fun OptimizationCardDarkPreview() {
    ClaudeCodeReviewTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            mockOptimizations.forEach { opt ->
                OptimizationCard(optimization = opt, isDark = true)
            }
        }
    }
}

@Preview(showBackground = true, name = "OptimizationCard – Light")
@Composable
private fun OptimizationCardLightPreview() {
    ClaudeCodeReviewTheme(darkTheme = false) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            mockOptimizations.forEach { opt ->
                OptimizationCard(optimization = opt, isDark = false)
            }
        }
    }
}
