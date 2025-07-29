package com.example.notitiddy

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var permissionButton: Button
    private lateinit var clearButton: Button
    private lateinit var notificationsRecyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter
    
    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val packageName = intent.getStringExtra(NotificationService.EXTRA_PACKAGE_NAME) ?: return
            
            when (intent.action) {
                NotificationService.NOTIFICATION_POSTED -> {
                    val appName = intent.getStringExtra(NotificationService.EXTRA_APP_NAME) ?: getAppNameFromPackage(packageName)
                    val title = intent.getStringExtra(NotificationService.EXTRA_TITLE)
                    val fullContent = intent.getStringExtra(NotificationService.EXTRA_CONTENT)
                    val contentIntent = intent.getParcelableExtra<PendingIntent>(NotificationService.EXTRA_CONTENT_INTENT)
                    // Use first 50 characters as short content for initial display
                    val shortContent = if (fullContent != null && fullContent.length > 50) {
                        fullContent.substring(0, 50) + "..."
                    } else {
                        fullContent
                    }
                    val notification = NotificationData(
                        packageName = packageName,
                        appName = appName,
                        title = title,
                        content = shortContent,
                        fullContent = fullContent,
                        timestamp = System.currentTimeMillis(),
                        contentIntent = contentIntent
                    )
                    notificationAdapter.addNotification(notification)
                }
                NotificationService.NOTIFICATION_REMOVED -> {
                    notificationAdapter.updateNotificationStatus(packageName)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Initialize views
        permissionButton = findViewById(R.id.permissionButton)
        clearButton = findViewById(R.id.clearButton)
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView)
        
        // Set up RecyclerView
        notificationAdapter = NotificationAdapter()
        notificationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = notificationAdapter
        }
        
        // Set up permission button
        permissionButton.setOnClickListener {
            if (!isNotificationServiceEnabled()) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            } else {
                Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Set up clear button
        clearButton.setOnClickListener {
            notificationAdapter.clearNotifications()
            Toast.makeText(this, "Notifications cleared", Toast.LENGTH_SHORT).show()
        }
        
        // Add test notification for testing rendering effects
        addTestNotification()
        
        // Register broadcast receiver
        val intentFilter = IntentFilter().apply {
            addAction(NotificationService.NOTIFICATION_POSTED)
            addAction(NotificationService.NOTIFICATION_REMOVED)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, intentFilter)
        
        // Update UI based on permission status
        updatePermissionStatus()
    }
    
    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }
    

    
    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver)
    }
    
    private fun updatePermissionStatus() {
        if (isNotificationServiceEnabled()) {
            permissionButton.text = getString(R.string.permission_granted)
            permissionButton.isEnabled = false
        } else {
            permissionButton.text = getString(R.string.grant_permission)
            permissionButton.isEnabled = true
        }
    }
    
    private fun isNotificationServiceEnabled(): Boolean {
        val packageName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":").toTypedArray()
            for (name in names) {
                if (packageName == name || name.startsWith("$packageName/")) {
                    return true
                }
            }
        }
        return false
    }
    
    private fun getAppNameFromPackage(packageName: String): String {
        return try {
            val packageManager = applicationContext.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName // Return package name if app name cannot be found
        }
    }
    
    private fun addTestNotification() {
        val longContent = "This is a very long notification content that should definitely be longer than 50 characters to test the expand button functionality. It contains multiple sentences and should trigger the expand button to appear when displayed in the notification list."
        val shortContent = longContent.substring(0, 50) + "..."
        
        val testNotification = NotificationData(
            packageName = "com.example.test",
            appName = "Test App",
            title = "Test Notification",
            content = shortContent,
            fullContent = longContent,
            timestamp = System.currentTimeMillis()
        )
        
        notificationAdapter.addNotification(testNotification)
    }

}