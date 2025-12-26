package com.wishlist.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.wishlist.app.MainActivity
import com.wishlist.app.R

/**
 * 알림 생성 및 관리를 위한 헬퍼 클래스
 */
object NotificationHelper {

    const val CHANNEL_ID = "wish_reminder_channel"
    const val CHANNEL_NAME = "위시 알림"
    const val CHANNEL_DESCRIPTION = "위시리스트 목표일 알림"

    const val PRICE_CHANNEL_ID = "price_tracking_channel"
    const val PRICE_CHANNEL_NAME = "가격 알림"
    const val PRICE_CHANNEL_DESCRIPTION = "가격 변동 및 할인 알림"

    /**
     * 알림 채널 생성 (Android 8.0+)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 위시 알림 채널
            val reminderChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(reminderChannel)

            // 가격 알림 채널
            val priceChannel = NotificationChannel(PRICE_CHANNEL_ID, PRICE_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = PRICE_CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(priceChannel)
        }
    }

    /**
     * 알림 빌드 및 표시
     */
    fun showNotification(
        context: Context,
        wishId: String,
        title: String,
        description: String,
        priority: Int
    ) {
        createNotificationChannel(context)

        // 알림 클릭 시 앱으로 이동하는 Intent
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            wishId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 우선순위에 따른 이모지 설정
        val priorityEmoji = when (priority) {
            3 -> "⭐⭐⭐"
            2 -> "⭐⭐"
            1 -> "⭐"
            else -> ""
        }

        val notificationTitle = "$priorityEmoji 위시 목표일 도달!"
        val notificationText = "\"$title\" - $description"

        // 알림 빌드
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // 알림 표시
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(wishId.hashCode(), notification)
    }

    /**
     * 가격 하락 알림
     */
    fun showPriceDropNotification(
        context: Context,
        wishId: String,
        title: String,
        oldPrice: Double,
        newPrice: Double,
        dropPercent: Double
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            wishId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formatter = java.text.DecimalFormat("#,###")
        val notificationTitle = "💰 가격 하락!"
        val notificationText = "\"$title\"\n${formatter.format(oldPrice)}원 → ${formatter.format(newPrice)}원 (${String.format("%.1f", dropPercent)}% 할인)"

        val notification = NotificationCompat.Builder(context, PRICE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(wishId.hashCode() + 1000, notification)
    }

    /**
     * 최저가 갱신 알림
     */
    fun showLowestPriceNotification(
        context: Context,
        wishId: String,
        title: String,
        price: Double
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            wishId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formatter = java.text.DecimalFormat("#,###")
        val notificationTitle = "🎉 역대 최저가!"
        val notificationText = "\"$title\"\n현재 가격: ${formatter.format(price)}원"

        val notification = NotificationCompat.Builder(context, PRICE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(wishId.hashCode() + 2000, notification)
    }
}
