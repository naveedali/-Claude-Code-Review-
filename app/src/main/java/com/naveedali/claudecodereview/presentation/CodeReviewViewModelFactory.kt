package com.naveedali.claudecodereview.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.naveedali.claudecodereview.domain.analyser.KotlinCodeAnalyser
import com.naveedali.claudecodereview.domain.repository.CodeReviewRepositoryImpl

/**
 * Manual [ViewModelProvider.Factory] for [CodeReviewViewModel].
 *
 * Why a factory?
 * ──────────────
 * [CodeReviewViewModel] takes a [com.naveedali.claudecodereview.domain.repository.CodeReviewRepository]
 * constructor parameter. Android's default ViewModelProvider cannot construct
 * it (it only supports zero-arg constructors), so we supply a factory that
 * builds the full dependency chain:
 *
 *   KotlinCodeAnalyser
 *         │
 *   CodeReviewRepositoryImpl(analyser)
 *         │
 *   CodeReviewViewModel(repository)
 *
 * Phase 3 migration path
 * ──────────────────────
 * To swap in an AI repository, replace [CodeReviewRepositoryImpl] with
 * `AiCodeReviewRepository(apiKey, httpClient)` — only this file changes.
 *
 * When to replace with a DI framework
 * ─────────────────────────────────────
 * This manual factory is fine for a small app. Once the dependency graph
 * grows (multiple ViewModels, shared services, configuration), consider
 * Hilt (`@HiltViewModel` + `@Inject`) which generates the factory automatically.
 */
class CodeReviewViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(CodeReviewViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        val analyser    = KotlinCodeAnalyser()
        val repository  = CodeReviewRepositoryImpl(analyser)
        return CodeReviewViewModel(repository) as T
    }
}
