package com.example.notitiddy

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notitiddy.repository.NotificationRepository
import com.example.notitiddy.database.NotificationDatabase
import com.example.notitiddy.NotificationData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationDetailActivity : AppCompatActivity(), NotificationDetailAdapter.OnNotificationClickListener {
    
    private lateinit var backButton: ImageView
    private lateinit var appIconImageView: ImageView
    private lateinit var appNameTextView: TextView
    private lateinit var notificationCountTextView: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationDetailAdapter
    private lateinit var repository: NotificationRepository
    
    companion object {
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_NAME = "app_name"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_detail)
        
        // Initialize repository
        val database = NotificationDatabase.getDatabase(this)
        repository = NotificationRepository(database.notificationDao())
        
        // Debug: Check total notifications in database
        repository.allNotifications.observe(this) { allNotifications ->
            android.util.Log.d("NotificationDetail", "Total notifications in database: ${allNotifications.size}")
        }
        
        initViews()
        setupRecyclerView()
        loadNotificationData()
    }
    
    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        appIconImageView = findViewById(R.id.detailAppIconImageView)
        appNameTextView = findViewById(R.id.detailAppNameTextView)
        notificationCountTextView = findViewById(R.id.detailNotificationCountTextView)
        recyclerView = findViewById(R.id.detailRecyclerView)
        
        backButton.setOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = NotificationDetailAdapter()
        adapter.setOnNotificationClickListener(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
    
    override fun onNotificationClicked(notificationId: Long) {
        // Mark notification as read in database
        CoroutineScope(Dispatchers.IO).launch {
            repository.markAsRead(notificationId)
            // Reload data on main thread to update the count
            CoroutineScope(Dispatchers.Main).launch {
                loadNotificationData()
                // Notify calling activity to refresh its data
                setResult(RESULT_OK)
            }
        }
    }
    
    private fun loadNotificationData() {
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: return
        
        appNameTextView.text = appName
        
        // Load app icon
        try {
            val packageManager = packageManager
            val appIcon = packageManager.getApplicationIcon(packageName)
            appIconImageView.setImageDrawable(appIcon)
        } catch (e: Exception) {
            appIconImageView.setImageResource(android.R.drawable.sym_def_app_icon)
        }
        
        // Get notifications for this package from repository
        android.util.Log.d("NotificationDetail", "Loading notifications for package: $packageName")
        android.util.Log.d("NotificationDetail", "Querying database for package: '$packageName'")
        repository.getNotificationsByPackage(packageName).observe(this) { notifications ->
            android.util.Log.d("NotificationDetail", "Received ${notifications.size} notifications for package: '$packageName'")
            notifications.forEach { item ->
                android.util.Log.d("NotificationDetail", "Found notification: package='${item.packageName}', title='${item.title}'")
            }
            val notificationDataList = notifications.map { item ->
                 NotificationData(
                      id = item.id,
                      packageName = item.packageName,
                      appName = item.appName,
                      title = item.title,
                      content = item.content,
                      fullContent = item.fullContent,
                      timestamp = item.postTime,
                      contentIntent = null, // We don't store PendingIntent in database
                      isRemoved = item.isRemoved,
                      isRead = item.isRead
                  )
             }
             adapter.updateNotifications(notificationDataList)
             // Count only unread notifications for display
             val unreadCount = notificationDataList.count { !it.isRead }
             notificationCountTextView.text = "$unreadCount unread notification${if (unreadCount != 1) "s" else ""}"
         }
    }
}