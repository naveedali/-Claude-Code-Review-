package com.naveedali.codereview.data.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * Thin wrapper around [SharedPreferences] for persisting LLM settings.
 *
 * Stored values
 * ─────────────
 *  • selected_provider  — name() of the active [LlmProvider] enum value
 *  • api_key_OPENAI     — OpenAI secret key
 *  • api_key_CLAUDE     — Anthropic secret key
 *  • api_key_GEMINI     — Google Gemini API key
 *
 * Why SharedPreferences instead of DataStore?
 * ────────────────────────────────────────────
 * SharedPreferences is synchronous and simpler — no coroutines needed to read
 * a value at ViewModel construction time.  DataStore is the modern replacement
 * but adds coroutine complexity that would obscure the educational focus here.
 * Migrating to DataStore is straightforward if needed later.
 *
 * @param context Application or Activity context (used to open the prefs file).
 */
class LlmPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    // ── Provider selection ────────────────────────────────────────────────────

    /**
     * The currently selected [LlmProvider].
     *
     * Defaults to [LlmProvider.OPENAI] when no value has been saved.
     * The setter persists the change immediately.
     */
    var selectedProvider: LlmProvider
        get() {
            val name = prefs.getString(KEY_SELECTED_PROVIDER, LlmProvider.OPENAI.name)
            // Safely parse — if an old/invalid value is stored, fall back to OPENAI
            return try {
                LlmProvider.valueOf(name ?: LlmProvider.OPENAI.name)
            } catch (e: IllegalArgumentException) {
                LlmProvider.OPENAI
            }
        }
        set(value) {
            prefs.edit().putString(KEY_SELECTED_PROVIDER, value.name).apply()
        }

    // ── API key CRUD ──────────────────────────────────────────────────────────

    /**
     * Returns the stored API key for [provider], or an empty string if none saved.
     */
    fun getApiKey(provider: LlmProvider): String =
        prefs.getString(apiKeyPrefKey(provider), "") ?: ""

    /**
     * Persists [key] for the given [provider].
     * Passing an empty string effectively clears the key.
     */
    fun saveApiKey(provider: LlmProvider, key: String) {
        prefs.edit().putString(apiKeyPrefKey(provider), key.trim()).apply()
    }

    /**
     * Returns the key for the currently active provider, or empty string.
     * Convenience shortcut used by the factory.
     */
    fun activeApiKey(): String = getApiKey(selectedProvider)

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Constructs the SharedPreferences key for a provider's API key. */
    private fun apiKeyPrefKey(provider: LlmProvider) = "${KEY_API_KEY_PREFIX}${provider.name}"

    companion object {
        private const val PREFS_FILE            = "llm_settings"
        private const val KEY_SELECTED_PROVIDER = "selected_provider"
        private const val KEY_API_KEY_PREFIX    = "api_key_"
    }
}
