package com.wishlist.app.data

/**
 * 위시리스트 정렬 옵션
 */
enum class SortOption(val displayName: String) {
    DATE_NEWEST("최신순"),
    DATE_OLDEST("오래된순"),
    PRICE_HIGH("가격 높은순"),
    PRICE_LOW("가격 낮은순"),
    PRIORITY_HIGH("우선순위 높은순"),
    CATEGORY("카테고리별"),
    TITLE("제목순")
}
