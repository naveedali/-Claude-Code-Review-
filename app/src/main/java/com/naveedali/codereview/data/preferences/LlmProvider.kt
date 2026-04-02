package com.naveedali.codereview.data.preferences

/**
 * The three supported AI providers.
 *
 * @param displayName   Human-readable name shown in the Settings screen.
 * @param modelName     Default model string sent in the API request.
 * @param keyHint       Short helper text shown below the API key field.
 * @param keyUrl        Where the user can obtain an API key.
 */
enum class LlmProvider(
    val displayName: String,
    val modelName: String,
    val keyHint: String,
    val keyUrl: String
) {
    OPENAI(
        displayName = "ChatGPT",
        modelName   = "gpt-4o",
        keyHint     = "sk-...",
        keyUrl      = "platform.openai.com/api-keys"
    ),
    CLAUDE(
        displayName = "Claude",
        modelName   = "claude-opus-4-5",
        keyHint     = "sk-ant-...",
        keyUrl      = "console.anthropic.com/settings/keys"
    ),
    GEMINI(
        displayName = "Gemini",
        modelName   = "gemini-1.5-pro",
        keyHint     = "AIza...",
        keyUrl      = "aistudio.google.com/app/apikey"
    )
}
