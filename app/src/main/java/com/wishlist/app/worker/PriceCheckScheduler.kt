package com.wishlist.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 가격 체크 작업 스케줄러
 */
object PriceCheckScheduler {

    private const val WORK_NAME = "price_check_work"

    /**
     * 주기적인 가격 체크 시작
     * @param context Context
     * @param intervalHours 체크 간격 (시간 단위, 기본 12시간)
     */
    fun schedulePriceCheck(context: Context, intervalHours: Long = 12) {
        // Wi-Fi 연결 시에만 실행되도록 제약 조건 설정
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 주기적 작업 요청 생성
        val workRequest = PeriodicWorkRequestBuilder<PriceCheckWorker>(
            intervalHours, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        // 작업 등록 (기존 작업이 있으면 유지)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * 가격 체크 중지
     */
    fun cancelPriceCheck(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /**
     * 즉시 가격 체크 실행 (테스트용)
     */
    fun runImmediateCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = androidx.work.OneTimeWorkRequestBuilder<PriceCheckWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
