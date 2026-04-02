package com.naveedali.claudecodereview.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.naveedali.claudecodereview.data.preferences.LlmPreferences
import com.naveedali.claudecodereview.data.preferences.LlmProvider
import com.naveedali.claudecodereview.ui.theme.ClaudeCodeReviewTheme

/**
 * Settings screen where the user picks an AI provider and enters their API keys.
 *
 * Design decisions
 * ────────────────
 * • Provider selection uses a [RadioButton] row inside a tappable card — gives
 *   more visual feedback than plain RadioButtons and is easier to tap on mobile.
 * • API key fields use [PasswordVisualTransformation] by default with a show/hide
 *   toggle — keys are sensitive and should not be visible by default.
 * • Keys are saved only on "Save" tap (not on every keystroke) — prevents
 *   half-typed keys from being persisted.
 * • [BackHandler] intercepts the system back gesture, same as the back arrow.
 *
 * State management
 * ────────────────
 * All mutable state is local to this composable — the settings are simple enough
 * that a dedicated ViewModel would be overkill.  [LlmPreferences] is read once
 * on composition and written on Save.
 *
 * @param isDarkTheme   Controls the top bar appearance.
 * @param onBack        Called when the user taps back arrow, Save, or system back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onBack: () -> Unit
) {
    // Intercept system back gesture — treat it the same as tapping the back arrow.
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val prefs   = remember { LlmPreferences(context) }

    // ── Local state — read from prefs once, written on Save ───────────────────
    var selectedProvider by remember { mutableStateOf(prefs.selectedProvider) }
    var openAiKey        by remember { mutableStateOf(prefs.getApiKey(LlmProvider.OPENAI)) }
    var claudeKey        by remember { mutableStateOf(prefs.getApiKey(LlmProvider.CLAUDE)) }
    var geminiKey        by remember { mutableStateOf(prefs.getApiKey(LlmProvider.GEMINI)) }

    // Toast-style confirmation after save
    var showSavedBanner by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // ── Provider selection ────────────────────────────────────────────
            SettingsSection(title = "AI Provider") {
                LlmProvider.entries.forEach { provider ->
                    ProviderCard(
                        provider    = provider,
                        isSelected  = selectedProvider == provider,
                        onSelect    = { selectedProvider = provider }
                    )
                }
            }

            // ── API Keys ──────────────────────────────────────────────────────
            SettingsSection(title = "API Keys") {
                Text(
                    text  = "Keys are stored only on this device and never shared.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                ApiKeyField(
                    provider = LlmProvider.OPENAI,
                    value    = openAiKey,
                    onChange = { openAiKey = it }
                )
                ApiKeyField(
                    provider = LlmProvider.CLAUDE,
                    value    = claudeKey,
                    onChange = { claudeKey = it }
                )
                ApiKeyField(
                    provider = LlmProvider.GEMINI,
                    value    = geminiKey,
                    onChange = { geminiKey = it }
                )
            }

            // ── Saved confirmation banner ─────────────────────────────────────
            if (showSavedBanner) {
                Surface(
                    color  = MaterialTheme.colorScheme.primaryContainer,
                    shape  = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier            = Modifier.padding(12.dp),
                        verticalAlignment   = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Check,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text  = "Settings saved. Using ${selectedProvider.displayName}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // ── Save button ───────────────────────────────────────────────────
            Button(
                onClick = {
                    prefs.selectedProvider = selectedProvider
                    prefs.saveApiKey(LlmProvider.OPENAI, openAiKey)
                    prefs.saveApiKey(LlmProvider.CLAUDE, claudeKey)
                    prefs.saveApiKey(LlmProvider.GEMINI, geminiKey)
                    showSavedBanner = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }
        }
    }
}

// ── Provider card ─────────────────────────────────────────────────────────────

/**
 * A tappable card showing a provider option with a [RadioButton].
 *
 * The entire card is clickable (not just the radio button) — improves tap
 * target size on mobile following Material Design guidelines.
 */
@Composable
private fun ProviderCard(
    provider:   LlmProvider,
    isSelected: Boolean,
    onSelect:   () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier            = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick  = onSelect
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = provider.displayName,
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    text  = provider.modelName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── API key field ─────────────────────────────────────────────────────────────

/**
 * Password-style text field for an API key.
 *
 * Features:
 *  • Hidden by default — shows dots until the show/hide icon is tapped.
 *  • Label and placeholder include the provider name so the user knows which
 *    key goes where.
 *  • Supporting text shows where to get the key (the provider's console URL).
 *
 * [PasswordVisualTransformation] vs [VisualTransformation.None]:
 * The toggle swaps between these two — the underlying text is always plain,
 * only the display changes.  This is the standard Compose password field pattern.
 */
@Composable
private fun ApiKeyField(
    provider: LlmProvider,
    value:    String,
    onChange: (String) -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value         = value,
        onValueChange = onChange,
        label         = { Text("${provider.displayName} API Key") },
        placeholder   = { Text(provider.keyHint, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
        supportingText = {
            Text(
                text  = "Get key at ${provider.keyUrl}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        visualTransformation = if (isVisible) VisualTransformation.None
                               else           PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { isVisible = !isVisible }) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.VisibilityOff
                                  else           Icons.Default.Visibility,
                    contentDescription = if (isVisible) "Hide key" else "Show key"
                )
            }
        },
        singleLine = true,
        modifier   = Modifier.fillMaxWidth()
    )
}

// ── Section wrapper ───────────────────────────────────────────────────────────

/**
 * A labelled section with a divider and consistent spacing.
 * Keeps each logical group visually separated without requiring a Card wrapper.
 */
@Composable
private fun SettingsSection(
    title:   String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Settings – Dark", widthDp = 400, heightDp = 800)
@Composable
private fun SettingsScreenDarkPreview() {
    ClaudeCodeReviewTheme(darkTheme = true) {
        SettingsScreen(isDarkTheme = true, onBack = {})
    }
}

@Preview(showBackground = true, name = "Settings – Light", widthDp = 400, heightDp = 800)
@Composable
private fun SettingsScreenLightPreview() {
    ClaudeCodeReviewTheme(darkTheme = false) {
        SettingsScreen(isDarkTheme = false, onBack = {})
    }
}
