package com.naveedali.codereview.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.naveedali.codereview.ui.theme.*

// ── Sample code shown on first launch ─────────────────────────────────────────
private val PLACEHOLDER_CODE = """fun fetchUser(id: String): User {
    val user = userRepository.find(id)
    println(user.name)          // ⚠ NPE risk

    val items = getItems()
    val names = items
        .filter { it.active }
        .map    { it.name }     // creates 2 lists

    val result = if (items.size > 0)
                    "found"
                 else if (items.size == 0)
                    "empty"
                 else
                    "unknown"   // unreachable

    for (item in items) {
        val sb = StringBuilder()
        sb.append(item.name)
        println(sb.toString())  // alloc inside loop
    }
    return user
}""".trimIndent()

/**
 * Styled code editor panel.
 *
 * Features
 * ────────
 *  • Dark/light-aware editor background (Catppuccin palette)
 *  • Line numbers that stay in sync via a shared vertical scroll state
 *  • Monospace font with consistent 20 sp line height
 *  • Horizontal scroll for long lines
 *  • "Review Code" button at the bottom
 *
 * @param code          Current code text (hoisted state).
 * @param onCodeChange  Called whenever the user edits text.
 * @param onReviewClick Called when the "Review Code" button is pressed.
 * @param isLoading     Disables the button and shows a progress indicator while
 *                      a review is in progress.
 * @param modifier      Standard Compose modifier.
 */
@Composable
fun CodeEditorPanel(
    code: String,
    onCodeChange: (String) -> Unit,
    onReviewClick: () -> Unit,
    isLoading: Boolean = false,
    isDark: Boolean = true,
    modifier: Modifier = Modifier
) {
    // ── Shared scroll so line numbers and code scroll together ────────────────
    val verticalScrollState   = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val editorBg      = if (isDark) EditorBgDark      else EditorBgLight
    val lineNumberBg  = if (isDark) LineNumberBgDark  else LineNumberBgLight
    val lineNumberFg  = if (isDark) LineNumberTextDark else LineNumberTextLight
    val codeTextColor = if (isDark) CodeTextDark       else CodeTextLight
    val cursorColor   = if (isDark) CursorColorDark    else CursorColorLight

    Column(modifier = modifier.fillMaxSize()) {

        // ── Panel header ──────────────────────────────────────────────────────
        PanelHeader(title = "Code Editor")

        // ── Editor area ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(editorBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScrollState)
                    .horizontalScroll(horizontalScrollState)
            ) {
                // ── Line numbers ──────────────────────────────────────────────
                LineNumberColumn(
                    lineCount  = code.lines().size,
                    textColor  = lineNumberFg,
                    background = lineNumberBg
                )

                // ── Code text field ───────────────────────────────────────────
                BasicTextField(
                    value           = code,
                    onValueChange   = onCodeChange,
                    textStyle       = CodeEditorTextStyle.copy(color = codeTextColor),
                    cursorBrush     = SolidColor(cursorColor),
                    modifier        = Modifier
                        .widthIn(min = 300.dp)
                        .padding(start = 12.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
                    decorationBox   = { innerTextField ->
                        // Show placeholder when code is empty
                        if (code.isEmpty()) {
                            Text(
                                text  = "// Paste or type your Kotlin code here…",
                                style = CodeEditorTextStyle.copy(
                                    color = lineNumberFg.copy(alpha = 0.6f)
                                )
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

        // ── Bottom bar: line count + Review button ────────────────────────────
        EditorBottomBar(
            lineCount     = code.lines().size,
            charCount     = code.length,
            isLoading     = isLoading,
            onReviewClick = onReviewClick
        )
    }
}

// ── Private sub-composables ───────────────────────────────────────────────────

/**
 * A labelled header strip shared by CodeEditorPanel and ResultsPanel.
 */
@Composable
internal fun PanelHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text  = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        trailingContent()
    }
}

/**
 * A column of right-aligned line numbers.
 *
 * The line height (20 sp) matches [CodeEditorTextStyle] exactly so numbers
 * stay vertically aligned with each code line.
 */
@Composable
private fun LineNumberColumn(
    lineCount: Int,
    textColor: androidx.compose.ui.graphics.Color,
    background: androidx.compose.ui.graphics.Color
) {
    val displayCount = maxOf(lineCount, 1)   // always show at least "1"

    Column(
        modifier              = Modifier
            .background(background)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment   = Alignment.End
    ) {
        repeat(displayCount) { index ->
            Text(
                text      = "${index + 1}",
                style     = LineNumberTextStyle.copy(color = textColor),
                textAlign = TextAlign.End
            )
        }
    }
}

/**
 * Status bar at the bottom of the editor showing metadata and the action button.
 */
@Composable
private fun EditorBottomBar(
    lineCount: Int,
    charCount: Int,
    isLoading: Boolean,
    onReviewClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Metadata
        Text(
            text  = "$lineCount lines  ·  $charCount chars",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Primary action
        Button(
            onClick  = onReviewClick,
            enabled  = !isLoading,
            shape    = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier  = Modifier.size(16.dp),
                    color     = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Analysing…")
            } else {
                Icon(
                    imageVector         = Icons.Default.PlayArrow,
                    contentDescription  = "Review code",
                    modifier            = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Review Code")
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Editor – Dark")
@Composable
private fun CodeEditorPanelDarkPreview() {
    ClaudeCodeReviewTheme(darkTheme = true) {
        CodeEditorPanel(
            code          = PLACEHOLDER_CODE,
            onCodeChange  = {},
            onReviewClick = {},
            isDark        = true,
            modifier      = Modifier
                .fillMaxWidth()
                .height(420.dp)
        )
    }
}

@Preview(showBackground = true, name = "Editor – Light")
@Composable
private fun CodeEditorPanelLightPreview() {
    ClaudeCodeReviewTheme(darkTheme = false) {
        CodeEditorPanel(
            code          = PLACEHOLDER_CODE,
            onCodeChange  = {},
            onReviewClick = {},
            isDark        = false,
            modifier      = Modifier
                .fillMaxWidth()
                .height(420.dp)
        )
    }
}
