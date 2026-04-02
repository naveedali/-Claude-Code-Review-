package com.naveedali.claudecodereview.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.naveedali.claudecodereview.data.preferences.LlmPreferences
import com.naveedali.claudecodereview.data.preferences.LlmProvider
import com.naveedali.claudecodereview.data.remote.AiCodeReviewRepository
import com.naveedali.claudecodereview.data.remote.ClaudeService
import com.naveedali.claudecodereview.data.remote.GeminiService
import com.naveedali.claudecodereview.data.remote.LlmService
import com.naveedali.claudecodereview.data.remote.OpenAiService
import com.naveedali.claudecodereview.domain.analyser.KotlinCodeAnalyser
import com.naveedali.claudecodereview.domain.repository.CodeReviewRepository
import com.naveedali.claudecodereview.domain.repository.CodeReviewRepositoryImpl

/**
 * Manual [ViewModelProvider.Factory] for [CodeReviewViewModel].
 *
 * Provider selection logic
 * ────────────────────────
 *  1. Read [LlmPreferences] → get selected provider + its API key.
 *  2. If the key is non-blank → create the matching [LlmService] → wrap in [AiCodeReviewRepository].
 *  3. If the key is blank    → fall back to offline [CodeReviewRepositoryImpl].
 *
 *  ┌──────────────────────────────────────────────────────────────┐
 *  │  Preferences      Key present?   Repository used             │
 *  ├──────────────────────────────────────────────────────────────┤
 *  │  OPENAI + key     yes            AiCodeReviewRepository      │
 *  │                                  (OpenAiService)             │
 *  │  CLAUDE + key     yes            AiCodeReviewRepository      │
 *  │                                  (ClaudeService)             │
 *  │  GEMINI + key     yes            AiCodeReviewRepository      │
 *  │                                  (GeminiService)             │
 *  │  any  + no key    no             CodeReviewRepositoryImpl    │
 *  │                                  (offline, rule-based)       │
 *  └──────────────────────────────────────────────────────────────┘
 *
 * @param context Used to open [SharedPreferences] via [LlmPreferences].
 *                Pass [LocalContext.current] from any Composable.
 */
class CodeReviewViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(CodeReviewViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        val repository = buildRepository()
        return CodeReviewViewModel(repository) as T
    }

    /**
     * Reads [LlmPreferences] and builds the appropriate [CodeReviewRepository].
     *
     * Called once when the ViewModel is first created — the result is cached
     * inside the ViewModel and survives configuration changes.
     */
    private fun buildRepository(): CodeReviewRepository {
        val prefs    = LlmPreferences(context)
        val provider = prefs.selectedProvider
        val apiKey   = prefs.activeApiKey()

        return if (apiKey.isNotBlank()) {
            // ── AI path ──────────────────────────────────────────────────────
            val service: LlmService = when (provider) {
                LlmProvider.OPENAI -> OpenAiService(apiKey = apiKey)
                LlmProvider.CLAUDE -> ClaudeService(apiKey = apiKey)
                LlmProvider.GEMINI -> GeminiService(apiKey = apiKey)
            }
            AiCodeReviewRepository(service)
        } else {
            // ── Offline fallback ─────────────────────────────────────────────
            // No key configured for the selected provider — run locally.
            CodeReviewRepositoryImpl(KotlinCodeAnalyser())
        }
    }
}
