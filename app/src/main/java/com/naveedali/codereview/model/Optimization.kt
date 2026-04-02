package com.naveedali.codereview.model

/**
 * Category of an optimization suggestion.
 *
 * Visual mapping (chip color):
 *  PERFORMANCE  → pink
 *  READABILITY  → cyan
 *  MEMORY       → green
 *  BEST_PRACTICE→ purple
 */
enum class OptimizationType {
    PERFORMANCE,
    READABILITY,
    MEMORY,
    BEST_PRACTICE
}

/**
 * A single optimization opportunity identified in the reviewed code.
 *
 * @param id          Unique identifier (used as LazyColumn key).
 * @param type        Category that drives the chip color.
 * @param title       Short headline.
 * @param description Why this optimization matters.
 * @param beforeCode  Optional snippet showing the original code.
 * @param afterCode   Optional snippet showing the improved code.
 */
data class Optimization(
    val id: String,
    val type: OptimizationType,
    val title: String,
    val description: String,
    val beforeCode: String? = null,
    val afterCode: String? = null
)
