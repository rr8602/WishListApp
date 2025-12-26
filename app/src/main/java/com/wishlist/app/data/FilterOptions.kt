package com.wishlist.app.data

/**
 * 필터링 옵션
 */
data class FilterOptions(
    val category: WishCategory? = null,  // null = 전체
    val completionStatus: CompletionFilter = CompletionFilter.ALL
)

/**
 * 완료 상태 필터
 */
enum class CompletionFilter(val displayName: String) {
    ALL("전체"),
    COMPLETED("완료"),
    INCOMPLETE("미완료")
}
