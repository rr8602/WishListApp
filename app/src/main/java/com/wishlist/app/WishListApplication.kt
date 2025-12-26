package com.wishlist.app

import android.app.Application
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import com.kakao.sdk.common.KakaoSdk
import java.security.MessageDigest

class WishListApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 카카오 SDK 초기화
        KakaoSdk.init(this, "86e22355c971bf22be29a529dc54c4a6")

        // 키 해시 출력 (디버깅용)
        printKeyHash()
    }

    private fun printKeyHash() {
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val keyHash = Base64.encodeToString(md.digest(), Base64.NO_WRAP)
                Log.d("KAKAO_KEY_HASH", "KeyHash: $keyHash")
                Log.d("KAKAO_KEY_HASH", "========================================")
                Log.d("KAKAO_KEY_HASH", "카카오 개발자 콘솔에 이 키 해시를 등록하세요:")
                Log.d("KAKAO_KEY_HASH", keyHash)
                Log.d("KAKAO_KEY_HASH", "========================================")
            }
        } catch (e: Exception) {
            Log.e("KAKAO_KEY_HASH", "Error getting key hash", e)
        }
    }
}
