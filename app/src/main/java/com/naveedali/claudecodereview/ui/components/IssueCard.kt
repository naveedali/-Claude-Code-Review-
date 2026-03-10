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
import com.naveedali.claudecodereview.model.CodeIssue
import com.naveedali.claudecodereview.model.IssueSeverity
import com.naveedali.claudecodereview.model.mockIssues
import com.naveedali.claudecodereview.ui.theme.*

// ── Severity → colours mapping ────────────────────────────────────────────────

private data class SeverityStyle(
    val accentColor: Color,
    val containerLight: Color,
    val containerDark: Color,
    val label: String
)

private val severityStyles = mapOf(
    IssueSeverity.ERROR   to SeverityStyle(ErrorRed,      ErrorContainerLight,   ErrorContainerDark,   "ERROR"),
    IssueSeverity.WARNING to SeverityStyle(WarningOrange,  WarningContainerLight, WarningContainerDark, "WARNING"),
    IssueSeverity.INFO    to SeverityStyle(InfoCyan,       InfoContainerLight,    InfoContainerDark,    "INFO"),
)

/**
 * A single expandable card representing one detected code issue.
 *
 * Tap anywhere on the card to toggle the suggestion section.
 *
 * @param issue    The [CodeIssue] to render.
 * @param isDark   Pass-through from the enclosing theme so container colours
 *                 pick the right dark/light variant.
 * @param modifier Standard Compose modifier.
 */
@Composable
fun IssueCard(
    issue: CodeIssue,
    isDark: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val style    = severityStyles[issue.severity] ?: severityStyles.getValue(IssueSeverity.INFO)
    val container = if (isDark) style.containerDark else style.containerLight

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(container)
            .border(
                width = 1.dp,
                color = style.accentColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        // ── Header row ────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Severity badge
            SeverityBadge(label = style.label, color = style.accentColor)

            // Line number pill
            issue.lineNumber?.let { line ->
                LinePill(lineNumber = line, color = style.accentColor)
            }

            // Title
            Text(
                text     = issue.title,
                style    = MaterialTheme.typography.titleSmall,
                color    = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Expand / collapse chevron (only when suggestion exists)
            if (issue.suggestion != null) {
                Icon(
                    imageVector        = if (expanded) Icons.Default.KeyboardArrowUp
                                         else           Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint               = style.accentColor,
                    modifier           = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // ── Description ───────────────────────────────────────────────────────
        Text(
            text  = issue.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        // ── Expandable suggestion ─────────────────────────────────────────────
        if (issue.suggestion != null) {
            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(
                        color     = style.accentColor.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment     = Alignment.Top
                    ) {
                        // "Fix" label
                        Text(
                            text  = "Fix:",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = style.accentColor
                            )
                        )
                        // Suggestion text
                        Text(
                            text  = issue.suggestion,
                            style = CodeSnippetTextStyle.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                            )
                        )
                    }
                }
            }
        }
    }
}

// ── Private helpers ───────────────────────────────────────────────────────────

/** Small pill that shows a severity label in the accent colour. */
@Composable
private fun SeverityBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium.copy(color = color)
        )
    }
}

/** Tiny "L:nn" pill showing the line number. */
@Composable
private fun LinePill(lineNumber: Int, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text  = "L:$lineNumber",
            style = MaterialTheme.typography.labelSmall.copy(color = color.copy(alpha = 0.9f))
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "IssueCard – Dark")
@Composable
private fun IssueCardDarkPreview() {
    ClaudeCodeReviewTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            mockIssues.forEach { issue ->
                IssueCard(issue = issue, isDark = true)
            }
        }
    }
}

@Preview(showBackground = true, name = "IssueCard – Light")
@Composable
private fun IssueCardLightPreview() {
    ClaudeCodeReviewTheme(darkTheme = false) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            mockIssues.forEach { issue ->
                IssueCard(issue = issue, isDark = false)
            }
        }
    }
}
