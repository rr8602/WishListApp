package com.wishlist.app.data

import java.util.UUID

/**
 * 위시리스트 아이템 데이터 클래스
 */
data class WishItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val imageUri: String? = null,
    val price: Double? = null,
    val category: WishCategory = WishCategory.OTHER,
    val priority: Int = 0,
    val targetDate: Long? = null,  // 목표일 (밀리초)
    val shoppingUrl: String? = null,  // 메인 쇼핑 링크
    val shoppingUrls: List<ShoppingUrlInfo> = emptyList(),  // 여러 쇼핑몰 비교용
    val priceHistory: List<PriceHistory> = emptyList(),  // 가격 변동 이력
    val priceTrackingEnabled: Boolean = false,  // 가격 추적 활성화
    val lowestPrice: Double? = null,  // 최저가 기록
    val userId: String? = null,  // 위시리스트 소유자 UID
    val giftInfo: GiftInfo? = null,  // 선물 정보
    val sharedWith: List<String> = emptyList(),  // 공유된 사용자 UID 목록
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)

/**
 * 위시리스트 카테고리
 */
enum class WishCategory(val displayName: String, val colorResId: Int) {
    ELECTRONICS("전자제품", android.R.color.holo_blue_light),
    FASHION("의류/패션", android.R.color.holo_purple),
    BOOKS("책/문구", android.R.color.holo_orange_light),
    TRAVEL("여행", android.R.color.holo_green_light),
    FOOD("음식", android.R.color.holo_red_light),
    HOBBY("취미", android.R.color.holo_blue_dark),
    HOME("생활용품", android.R.color.holo_orange_dark),
    OTHER("기타", android.R.color.darker_gray)
}
