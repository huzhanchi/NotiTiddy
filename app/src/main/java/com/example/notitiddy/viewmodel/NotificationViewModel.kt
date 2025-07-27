package com.example.notitiddy.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.notitiddy.database.NotificationDatabase
import com.example.notitiddy.model.NotificationItem
import com.example.notitiddy.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel for handling notification-related UI data
 */
class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: NotificationRepository
    val allNotifications: LiveData<List<NotificationItem>>
    val activeNotifications: LiveData<List<NotificationItem>>
    
    init {
        val notificationDao = NotificationDatabase.getDatabase(application).notificationDao()
        repository = NotificationRepository(notificationDao)
        allNotifications = repository.allNotifications
        activeNotifications = repository.activeNotifications
    }
    
    fun insert(notification: NotificationItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(notification)
    }
    
    fun update(notification: NotificationItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(notification)
    }
    
    fun delete(notification: NotificationItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(notification)
    }
    
    fun deleteAll() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAll()
    }
    
    fun getNotificationsByPackage(packageName: String): LiveData<List<NotificationItem>> {
        return repository.getNotificationsByPackage(packageName)
    }
    
    fun markAsRead(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        repository.markAsRead(id)
    }
    
    fun markAsRemoved(packageName: String) = viewModelScope.launch(Dispatchers.IO) {
        repository.markAsRemoved(packageName)
    }
}