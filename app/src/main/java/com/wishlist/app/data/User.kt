package com.wishlist.app.data

/**
 * 사용자 데이터 모델
 */
data class User(
    val uid: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val profileImageUrl: String? = null,
    val provider: LoginProvider = LoginProvider.EMAIL,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 로그인 제공자
 */
enum class LoginProvider {
    EMAIL,      // 이메일 로그인
    GOOGLE,     // 구글 로그인
    KAKAO,      // 카카오 로그인
    NAVER       // 네이버 로그인
}

/**
 * 선물 정보 (누가 선물했는지)
 */
data class GiftInfo(
    val giftedBy: String? = null,        // 선물한 사용자 UID
    val giftedByName: String? = null,    // 선물한 사용자 이름
    val giftedAt: Long? = null,          // 선물한 시간
    val message: String? = null          // 선물 메시지
)
