package com.example.notitiddy

import android.app.Notification
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.notitiddy.database.NotificationDatabase
import com.example.notitiddy.model.NotificationItem
import com.example.notitiddy.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationService : NotificationListenerService() {

    private lateinit var repository: NotificationRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val TAG = "NotificationService"
        const val NOTIFICATION_POSTED = "com.example.notitiddy.NOTIFICATION_POSTED"
        const val NOTIFICATION_REMOVED = "com.example.notitiddy.NOTIFICATION_REMOVED"
        const val EXTRA_NOTIFICATION = "extra_notification"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CONTENT = "extra_content"
    }

    override fun onCreate() {
        super.onCreate()
        val notificationDao = NotificationDatabase.getDatabase(this).notificationDao()
        repository = NotificationRepository(notificationDao)
        Log.d(TAG, "NotificationService created")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val notification = sbn.notification
        val extras = notification.extras
        
        // Get app name
        val appName = try {
            val packageManager = applicationContext.packageManager
            val applicationInfo: ApplicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
        
        // Extract notification details
        val title = extras.getString(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        
        // Get the most complete content available
        val fullContent = bigText ?: textLines?.joinToString("\n") ?: text
        
        Log.d(TAG, "Notification posted: $packageName, $title, $text")
        
        // Store notification in database
        serviceScope.launch {
            val notificationItem = NotificationItem(
                packageName = packageName,
                appName = appName,
                title = title,
                content = text,
                fullContent = fullContent,
                postTime = sbn.postTime,
                isRead = false,
                isRemoved = false
            )
            repository.insert(notificationItem)
        }
        
        // Broadcast notification posted event
        val intent = Intent(NOTIFICATION_POSTED).apply {
            putExtra(EXTRA_NOTIFICATION, packageName)
            putExtra(EXTRA_PACKAGE_NAME, packageName)
            putExtra(EXTRA_APP_NAME, appName)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_CONTENT, fullContent)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        
        Log.d(TAG, "Notification removed: $packageName")
        
        // Mark notification as removed in database
        serviceScope.launch {
            repository.markAsRemoved(packageName)
        }
        
        // Broadcast notification removed event
        val intent = Intent(NOTIFICATION_REMOVED).apply {
            putExtra(EXTRA_NOTIFICATION, packageName)
            putExtra(EXTRA_PACKAGE_NAME, packageName)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }
}