package com.wishlist.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 알람 시간이 도달했을 때 알림을 표시하는 BroadcastReceiver
 */
class ReminderBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_WISH_ID = "wish_id"
        const val EXTRA_WISH_TITLE = "wish_title"
        const val EXTRA_WISH_DESCRIPTION = "wish_description"
        const val EXTRA_WISH_PRIORITY = "wish_priority"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val wishId = intent.getStringExtra(EXTRA_WISH_ID) ?: return
        val title = intent.getStringExtra(EXTRA_WISH_TITLE) ?: "위시 아이템"
        val description = intent.getStringExtra(EXTRA_WISH_DESCRIPTION) ?: ""
        val priority = intent.getIntExtra(EXTRA_WISH_PRIORITY, 2)

        // 알림 표시
        NotificationHelper.showNotification(
            context = context,
            wishId = wishId,
            title = title,
            description = description,
            priority = priority
        )
    }
}
