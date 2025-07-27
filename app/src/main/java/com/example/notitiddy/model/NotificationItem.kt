package com.example.notitiddy.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Data model representing a notification item
 */
@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String?,
    val content: String?,
    val fullContent: String?,
    val postTime: Long,
    val isRead: Boolean = false,
    val isRemoved: Boolean = false,
    val timestamp: Long = Date().time
)