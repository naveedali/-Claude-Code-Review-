package com.naveedali.claudecodereview.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.naveedali.claudecodereview.BuildConfig
import com.naveedali.claudecodereview.data.remote.OpenAiCodeReviewRepository
import com.naveedali.claudecodereview.data.remote.OpenAiService
import com.naveedali.claudecodereview.domain.analyser.KotlinCodeAnalyser
import com.naveedali.claudecodereview.domain.repository.CodeReviewRepository
import com.naveedali.claudecodereview.domain.repository.CodeReviewRepositoryImpl

/**
 * Manual [ViewModelProvider.Factory] for [CodeReviewViewModel].
 *
 * Phase 3 — Smart repository selection
 * ────────────────────────────────────
 * The factory reads [BuildConfig.OPENAI_API_KEY] (populated from local.properties)
 * and selects the appropriate [CodeReviewRepository] implementation at runtime:
 *
 *  ┌────────────────────────────────────────────────────────┐
 *  │  OPENAI_API_KEY present  →  OpenAiCodeReviewRepository  │
 *  │  Key blank / missing     →  CodeReviewRepositoryImpl    │
 *  │                             (local rule-based fallback) │
 *  └────────────────────────────────────────────────────────┘
 *
 * This means:
 *  • Developers can build and run the app without an API key — the
 *    rule-based analyser handles everything locally.
 *  • Adding a key to local.properties instantly switches to GPT-4o,
 *    with zero code changes needed.
 *
 * Dependency graph
 * ────────────────
 *
 *   With AI key:
 *     OpenAiService(apiKey)
 *           │
 *     OpenAiCodeReviewRepository(service)
 *           │
 *     CodeReviewViewModel(repository)
 *
 *   Without AI key:
 *     KotlinCodeAnalyser
 *           │
 *     CodeReviewRepositoryImpl(analyser)
 *           │
 *     CodeReviewViewModel(repository)
 *
 * Migration to Hilt
 * ─────────────────
 * This manual factory is fine for a small app.  When the dependency graph grows,
 * replace with @HiltViewModel + @Inject — Hilt generates the factory automatically
 * and handles the BuildConfig conditional via a @Provides method in a Module.
 */
class CodeReviewViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(CodeReviewViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        val repository = buildRepository()
        return CodeReviewViewModel(repository) as T
    }

    /**
     * Builds the correct [CodeReviewRepository] based on whether an API key exists.
     *
     * [BuildConfig.OPENAI_API_KEY] is an empty string when:
     *  • The key is missing from local.properties entirely.
     *  • The key line exists but has no value: `OPENAI_API_KEY=`
     */
    private fun buildRepository(): CodeReviewRepository {
        val apiKey = BuildConfig.OPENAI_API_KEY

        return if (apiKey.isNotBlank()) {
            // ── AI path ──────────────────────────────────────────────────────
            // Key is present → use GPT-4o for real analysis and refactoring.
            val service = OpenAiService(apiKey = apiKey)
            OpenAiCodeReviewRepository(service)
        } else {
            // ── Local fallback path ──────────────────────────────────────────
            // No key → use the offline rule-based analyser (works without network).
            val analyser = KotlinCodeAnalyser()
            CodeReviewRepositoryImpl(analyser)
        }
    }
}
