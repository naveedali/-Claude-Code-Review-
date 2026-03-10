package com.naveedali.claudecodereview.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.naveedali.claudecodereview.model.CodeIssue
import com.naveedali.claudecodereview.model.Optimization
import com.naveedali.claudecodereview.model.ReviewResult
import com.naveedali.claudecodereview.model.mockReviewResult
import com.naveedali.claudecodereview.ui.theme.*

// ── Tab indices ───────────────────────────────────────────────────────────────
private const val TAB_ISSUES        = 0
private const val TAB_OPTIMIZATIONS = 1

/**
 * Results panel — shows issues and optimisations discovered in the reviewed
 * code, plus a prominent "Refactor" action button.
 *
 * Layout
 * ──────
 *  ┌─────────────────────────────────────────────┐
 *  │  Panel header  (summary line + counts)       │
 *  ├──────────────────┬──────────────────────────┤
 *  │  Issues (n)  │  Optimizations (n)           │  ← TabRow
 *  ├─────────────────────────────────────────────┤
 *  │  Scrollable card list                        │
 *  ├─────────────────────────────────────────────┤
 *  │  [ ✦ Refactor Code ]                         │  ← bottom bar
 *  └─────────────────────────────────────────────┘
 *
 * @param result          The [ReviewResult] to display.
 * @param onRefactorClick Called when the Refactor button is pressed.
 * @param isDark          Dark/light pass-through for child cards.
 * @param modifier        Standard Compose modifier.
 */
@Composable
fun ResultsPanel(
    result: ReviewResult,
    onRefactorClick: () -> Unit,
    isDark: Boolean = true,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(TAB_ISSUES) }

    Column(modifier = modifier.fillMaxSize()) {

        // ── Header: title + summary ───────────────────────────────────────────
        PanelHeader(title = "Review Results") {
            if (result.summary.isNotBlank()) {
                Text(
                    text  = result.summary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Tab row ───────────────────────────────────────────────────────────
        ResultsTabRow(
            issueCount        = result.issues.size,
            optimizationCount = result.optimizations.size,
            selectedTab       = selectedTab,
            onTabSelected     = { selectedTab = it }
        )

        HorizontalDivider(
            color     = MaterialTheme.colorScheme.outline,
            thickness = 1.dp
        )

        // ── Card list ─────────────────────────────────────────────────────────
        // Each list composable renders its own empty state so we stay in a
        // simple Box scope and avoid Kotlin's implicit-receiver ambiguity with
        // ColumnScope.AnimatedVisibility.
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                TAB_ISSUES -> IssuesList(
                    issues = result.issues,
                    isDark = isDark
                )
                TAB_OPTIMIZATIONS -> OptimizationsList(
                    optimizations = result.optimizations,
                    isDark        = isDark
                )
            }
        }

        HorizontalDivider(
            color     = MaterialTheme.colorScheme.outline,
            thickness = 1.dp
        )

        // ── Refactor button bar ───────────────────────────────────────────────
        RefactorBar(onRefactorClick = onRefactorClick)
    }
}

// ── Private sub-composables ───────────────────────────────────────────────────

@Composable
private fun ResultsTabRow(
    issueCount: Int,
    optimizationCount: Int,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    TabRow(
        selectedTabIndex = selectedTab,
        containerColor   = MaterialTheme.colorScheme.surfaceVariant,
        contentColor     = MaterialTheme.colorScheme.primary
    ) {
        Tab(
            selected = selectedTab == TAB_ISSUES,
            onClick  = { onTabSelected(TAB_ISSUES) },
            text     = {
                BadgedTab(
                    label = "Issues",
                    count = issueCount,
                    badgeColor = if (issueCount > 0) ErrorRed else MaterialTheme.colorScheme.outline
                )
            }
        )
        Tab(
            selected = selectedTab == TAB_OPTIMIZATIONS,
            onClick  = { onTabSelected(TAB_OPTIMIZATIONS) },
            text     = {
                BadgedTab(
                    label      = "Optimizations",
                    count      = optimizationCount,
                    badgeColor = if (optimizationCount > 0) OptBestPurple
                                 else MaterialTheme.colorScheme.outline
                )
            }
        )
    }
}

/** Tab label with a coloured count badge. */
@Composable
private fun BadgedTab(
    label: String,
    count: Int,
    badgeColor: androidx.compose.ui.graphics.Color
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        if (count > 0) {
            Box(
                modifier        = Modifier
                    .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(50))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "$count",
                    style = MaterialTheme.typography.labelSmall.copy(color = badgeColor)
                )
            }
        }
    }
}

/**
 * Scrollable list of [IssueCard]s.
 * Shows its own empty state when [issues] is empty — keeps the list
 * self-contained and avoids scope-related AnimatedVisibility issues.
 */
@Composable
private fun IssuesList(
    issues: List<CodeIssue>,
    isDark: Boolean
) {
    if (issues.isEmpty()) {
        EmptyState(
            icon    = Icons.Default.Warning,
            message = "No issues detected.\nYour code looks clean!"
        )
        return
    }
    LazyColumn(
        contentPadding      = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier            = Modifier.fillMaxSize()
    ) {
        items(items = issues, key = { it.id }) { issue ->
            IssueCard(issue = issue, isDark = isDark)
        }
    }
}

/**
 * Scrollable list of [OptimizationCard]s.
 * Shows its own empty state when [optimizations] is empty.
 */
@Composable
private fun OptimizationsList(
    optimizations: List<Optimization>,
    isDark: Boolean
) {
    if (optimizations.isEmpty()) {
        EmptyState(
            icon    = Icons.Default.Info,
            message = "No optimisations found."
        )
        return
    }
    LazyColumn(
        contentPadding      = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier            = Modifier.fillMaxSize()
    ) {
        items(items = optimizations, key = { it.id }) { opt ->
            OptimizationCard(optimization = opt, isDark = isDark)
        }
    }
}

/** Centred placeholder shown when a list is empty. */
@Composable
private fun EmptyState(icon: ImageVector, message: String) {
    Column(
        modifier              = Modifier.fillMaxSize(),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.Center
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            modifier           = Modifier.size(40.dp),
            tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text  = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

/**
 * Bottom bar containing the primary "Refactor Code" button.
 *
 * The Refactor action is intentionally separated from the Review action:
 * users should be able to review first, understand the suggestions, and then
 * decide whether to apply the automated refactoring.
 */
@Composable
private fun RefactorBar(onRefactorClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Button(
            onClick  = onRefactorClick,
            shape    = RoundedCornerShape(8.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = BrandPrimary,
                contentColor   = BrandOnPrimary
            ),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.Build,
                contentDescription = "Refactor code",
                modifier           = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text  = "Refactor Code",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "ResultsPanel – Dark", widthDp = 380, heightDp = 600)
@Composable
private fun ResultsPanelDarkPreview() {
    ClaudeCodeReviewTheme(darkTheme = true) {
        ResultsPanel(
            result          = mockReviewResult,
            onRefactorClick = {},
            isDark          = true
        )
    }
}

@Preview(showBackground = true, name = "ResultsPanel – Light", widthDp = 380, heightDp = 600)
@Composable
private fun ResultsPanelLightPreview() {
    ClaudeCodeReviewTheme(darkTheme = false) {
        ResultsPanel(
            result          = mockReviewResult,
            onRefactorClick = {},
            isDark          = false
        )
    }
}
