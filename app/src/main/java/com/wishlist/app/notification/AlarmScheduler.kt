package com.wishlist.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wishlist.app.data.WishItem

/**
 * AlarmManager를 사용하여 알림 스케줄링을 관리하는 클래스
 */
object AlarmScheduler {

    /**
     * 위시 아이템에 대한 알람 스케줄링
     */
    fun scheduleReminder(context: Context, wish: WishItem) {
        val targetDate = wish.targetDate ?: return

        // 과거 날짜는 스케줄링하지 않음
        if (targetDate <= System.currentTimeMillis()) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(ReminderBroadcastReceiver.EXTRA_WISH_ID, wish.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_WISH_TITLE, wish.title)
            putExtra(ReminderBroadcastReceiver.EXTRA_WISH_DESCRIPTION, wish.description)
            putExtra(ReminderBroadcastReceiver.EXTRA_WISH_PRIORITY, wish.priority)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            wish.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Android 12+ (API 31+)에서는 SCHEDULE_EXACT_ALARM 권한 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    targetDate,
                    pendingIntent
                )
            } else {
                // 정확한 알람 권한이 없으면 대략적인 알람 사용
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    targetDate,
                    pendingIntent
                )
            }
        } else {
            // Android 12 이전 버전
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                targetDate,
                pendingIntent
            )
        }
    }

    /**
     * 위시 아이템의 알람 취소
     */
    fun cancelReminder(context: Context, wishId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            wishId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
