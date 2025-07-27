package com.example.notitiddy.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.notitiddy.model.NotificationItem

/**
 * Data Access Object for the notifications table
 */
@Dao
interface NotificationDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationItem): Long
    
    @Update
    suspend fun update(notification: NotificationItem)
    
    @Delete
    suspend fun delete(notification: NotificationItem)
    
    @Query("DELETE FROM notifications")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): LiveData<List<NotificationItem>>
    
    @Query("SELECT * FROM notifications WHERE isRemoved = 0 ORDER BY timestamp DESC")
    fun getActiveNotifications(): LiveData<List<NotificationItem>>
    
    @Query("SELECT * FROM notifications WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getNotificationsByPackage(packageName: String): LiveData<List<NotificationItem>>
    
    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun getNotificationById(id: Long): NotificationItem?
    
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)
    
    @Query("UPDATE notifications SET isRemoved = 1 WHERE packageName = :packageName")
    suspend fun markAsRemoved(packageName: String)
}