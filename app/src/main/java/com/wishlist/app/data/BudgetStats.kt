package com.wishlist.app.data

/**
 * 예산 통계
 */
data class BudgetStats(
    val totalBudget: Double,
    val completedBudget: Double,
    val incompleteBudget: Double,
    val categoryBudgets: Map<WishCategory, Double>
)
