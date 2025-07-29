package com.example.notitiddy

import android.app.PendingIntent
import java.util.Date

data class NotificationData(
    val packageName: String,
    val appName: String,
    val title: String?,
    val content: String?,
    val fullContent: String?,
    val timestamp: Long,
    val contentIntent: PendingIntent? = null,
    val isRemoved: Boolean = false
) {
    val date: Date
        get() = Date(timestamp)
}