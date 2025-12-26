package com.wishlist.app.data

/**
 * 가격 이력 데이터 클래스
 * 특정 시점의 가격 정보를 저장
 */
data class PriceHistory(
    val price: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "" // 쇼핑몰 이름
)

/**
 * 여러 쇼핑몰의 가격 정보
 */
data class ShoppingUrlInfo(
    val url: String,
    val siteName: String, // 쿠팡, 네이버쇼핑 등
    val currentPrice: Double? = null,
    val lastChecked: Long? = null,
    val priceHistory: List<PriceHistory> = emptyList()
)
