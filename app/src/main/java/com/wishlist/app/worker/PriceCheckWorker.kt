package com.wishlist.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wishlist.app.data.PriceHistory
import com.wishlist.app.data.WishRepository
import com.wishlist.app.notification.NotificationHelper
import com.wishlist.app.utils.PriceTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 백그라운드 가격 체크 Worker
 * 주기적으로 위시리스트의 가격을 확인하고 변동이 있으면 알림
 */
class PriceCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = WishRepository(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val wishes = repository.getAllWishes()

            // 가격 추적이 활성화된 위시만 확인
            val trackingWishes = wishes.filter {
                it.priceTrackingEnabled && !it.shoppingUrl.isNullOrEmpty()
            }

            for (wish in trackingWishes) {
                try {
                    val currentPrice = PriceTracker.fetchPrice(wish.shoppingUrl!!)

                    if (currentPrice != null) {
                        // 가격 이력에 추가
                        val newHistory = wish.priceHistory + PriceHistory(
                            price = currentPrice,
                            timestamp = System.currentTimeMillis(),
                            source = PriceTracker.detectShoppingSite(wish.shoppingUrl)
                        )

                        // 최저가 갱신
                        val lowestPrice = wish.lowestPrice?.let { minOf(it, currentPrice) } ?: currentPrice

                        // 가격 하락 감지
                        val previousPrice = wish.priceHistory.lastOrNull()?.price
                        val priceDropped = previousPrice != null && currentPrice < previousPrice
                        val priceDropPercent = if (previousPrice != null && previousPrice > 0) {
                            ((previousPrice - currentPrice) / previousPrice) * 100
                        } else {
                            0.0
                        }

                        // WishItem 업데이트
                        val updatedWish = wish.copy(
                            price = currentPrice,
                            priceHistory = newHistory,
                            lowestPrice = lowestPrice
                        )
                        repository.updateWish(updatedWish)

                        // 가격이 5% 이상 하락했으면 알림 (previousPrice가 null이 아닐 때만)
                        if (priceDropped && priceDropPercent >= 5.0 && previousPrice != null) {
                            NotificationHelper.showPriceDropNotification(
                                applicationContext,
                                wish.id,
                                wish.title,
                                previousPrice,
                                currentPrice,
                                priceDropPercent
                            )
                        }

                        // 최저가 갱신 시 알림
                        if (currentPrice == lowestPrice && wish.priceHistory.size > 1) {
                            NotificationHelper.showLowestPriceNotification(
                                applicationContext,
                                wish.id,
                                wish.title,
                                currentPrice
                            )
                        }
                    }
                } catch (e: Exception) {
                    // 개별 아이템 에러는 무시하고 계속 진행
                    e.printStackTrace()
                }

                // 너무 빠른 요청 방지 (1초 대기)
                kotlinx.coroutines.delay(1000)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
