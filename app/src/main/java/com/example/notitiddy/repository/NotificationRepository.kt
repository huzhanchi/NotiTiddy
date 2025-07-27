package com.example.notitiddy.repository

import androidx.lifecycle.LiveData
import com.example.notitiddy.database.NotificationDao
import com.example.notitiddy.model.NotificationItem

/**
 * Repository for handling notification data operations
 */
class NotificationRepository(private val notificationDao: NotificationDao) {
    
    val allNotifications: LiveData<List<NotificationItem>> = notificationDao.getAllNotifications()
    val activeNotifications: LiveData<List<NotificationItem>> = notificationDao.getActiveNotifications()
    
    suspend fun insert(notification: NotificationItem): Long {
        return notificationDao.insert(notification)
    }
    
    suspend fun update(notification: NotificationItem) {
        notificationDao.update(notification)
    }
    
    suspend fun delete(notification: NotificationItem) {
        notificationDao.delete(notification)
    }
    
    suspend fun deleteAll() {
        notificationDao.deleteAll()
    }
    
    fun getNotificationsByPackage(packageName: String): LiveData<List<NotificationItem>> {
        return notificationDao.getNotificationsByPackage(packageName)
    }
    
    suspend fun getNotificationById(id: Long): NotificationItem? {
        return notificationDao.getNotificationById(id)
    }
    
    suspend fun markAsRead(id: Long) {
        notificationDao.markAsRead(id)
    }
    
    suspend fun markAsRemoved(packageName: String) {
        notificationDao.markAsRemoved(packageName)
    }
}