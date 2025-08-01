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
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import com.google.android.material.navigation.NavigationView
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import com.example.notitiddy.database.NotificationDatabase
import com.example.notitiddy.repository.NotificationRepository

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, GroupedNotificationAdapter.OnItemClickListener {
    private lateinit var notificationsRecyclerView: RecyclerView
    private lateinit var notificationAdapter: GroupedNotificationAdapter
    private lateinit var headerLayout: LinearLayout
    private lateinit var searchBarLayout: LinearLayout
    private lateinit var searchIcon: ImageView
    private lateinit var backIcon: ImageView
    private lateinit var searchEditText: EditText
    private lateinit var clearSearchIcon: ImageView
    private lateinit var menuIcon: ImageView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private var isSearchVisible = false
    private lateinit var repository: NotificationRepository
    
    private val detailActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // Refresh the notification data when returning from detail activity
            loadNotificationsFromDatabase()
            notificationAdapter.refreshData()
        }
    }
    
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
                        id = 0, // Will be set when saved to database
                        packageName = packageName,
                        appName = appName,
                        title = title,
                        content = shortContent,
                        fullContent = fullContent,
                        timestamp = System.currentTimeMillis(),
                        contentIntent = contentIntent,
                        isRead = false // New notifications are unread
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
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView)
        headerLayout = findViewById(R.id.headerLayout)
        searchBarLayout = findViewById(R.id.searchBarLayout)
        searchIcon = findViewById(R.id.searchIcon)
        backIcon = findViewById(R.id.backIcon)
        searchEditText = findViewById(R.id.searchEditText)
        clearSearchIcon = findViewById(R.id.clearSearchIcon)
        menuIcon = findViewById(R.id.menuIcon)
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        
        // Set up navigation drawer
        setupNavigationDrawer()
        
        // Set up search functionality
        setupSearchFunctionality()
        
        // Initialize repository
        val database = NotificationDatabase.getDatabase(this)
        repository = NotificationRepository(database.notificationDao())
        
        // Set up RecyclerView
        notificationAdapter = GroupedNotificationAdapter()
        notificationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = notificationAdapter
            
            // Enable smooth animations for expand/collapse
            itemAnimator = DefaultItemAnimator().apply {
                addDuration = 300
                removeDuration = 300
                moveDuration = 300
                changeDuration = 300
            }
        }
        
        // Set up click listener for notification items
        notificationAdapter.setOnItemClickListener(this)
        
        // Load notifications from database
        loadNotificationsFromDatabase()
        
        // Add test notification for testing rendering effects
        addTestNotification()
        
        // Register broadcast receiver
        val intentFilter = IntentFilter().apply {
            addAction(NotificationService.NOTIFICATION_POSTED)
            addAction(NotificationService.NOTIFICATION_REMOVED)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, intentFilter)
        
    }

    private fun setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(this)
        
        // Set up hamburger menu click listener
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(navigationView)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_notifications -> {
                // Already on notifications screen
                Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_settings -> {
                Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
                // TODO: Navigate to settings screen
            }
            R.id.nav_about -> {
                Toast.makeText(this, "About", Toast.LENGTH_SHORT).show()
                // TODO: Show about dialog
            }
        }
        drawerLayout.closeDrawer(navigationView)
        return true
    }

    private fun setupSearchFunctionality() {
        // Initially hide search bar
        searchBarLayout.visibility = View.GONE
        
        // Search icon click listener
        searchIcon.setOnClickListener {
            showSearchBar()
        }
        
        // Back icon click listener
        backIcon.setOnClickListener {
            hideSearchBar()
        }
        
        // Clear search icon click listener
        clearSearchIcon.setOnClickListener {
            searchEditText.text.clear()
        }
        
        // Search text change listener
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterNotifications(s.toString())
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    private fun showSearchBar() {
        isSearchVisible = true
        searchBarLayout.visibility = View.VISIBLE
        searchEditText.requestFocus()
    }
    
    private fun hideSearchBar() {
        isSearchVisible = false
        searchBarLayout.visibility = View.GONE
        searchEditText.text.clear()
        // Reset filter to show all notifications
        notificationAdapter.filter("")
    }
    
    private fun filterNotifications(query: String) {
        notificationAdapter.filter(query)
    }
    
    private fun loadNotificationsFromDatabase() {
        repository.allNotifications.observe(this) { notificationItems ->
            android.util.Log.d("MainActivity", "Loaded ${notificationItems.size} notifications from database")
            
            // Clear existing notifications and add from database
            notificationAdapter.clearNotifications()
            
            // Convert NotificationItem to NotificationData and add to adapter
            notificationItems.forEach { item ->
                val notificationData = NotificationData(
                    id = item.id,
                    packageName = item.packageName,
                    appName = item.appName,
                    title = item.title,
                    content = item.content,
                    fullContent = item.fullContent,
                    timestamp = item.postTime,
                    contentIntent = null, // NotificationItem doesn't store contentIntent
                    isRead = item.isRead
                )
                notificationAdapter.addNotification(notificationData)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver)
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
        val currentTime = System.currentTimeMillis()
        
        // Create multiple test notifications from different apps
        val testNotifications = listOf(
            // WhatsApp notifications (3 notifications)
            NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "John Doe",
                content = "Hey! Are you free for lunch today? I was thinking we could try that new restaurant downtown.",
                fullContent = "Hey! Are you free for lunch today? I was thinking we could try that new restaurant downtown. Let me know if you're interested!",
                timestamp = currentTime - 60000 // 1 minute ago
            ),
            NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Sarah Wilson",
                content = "Don't forget about the meeting tomorrow at 3 PM. We need to discuss the project timeline and deliverables.",
                fullContent = "Don't forget about the meeting tomorrow at 3 PM. We need to discuss the project timeline and deliverables. Please bring your notes from last week's discussion.",
                timestamp = currentTime - 300000 // 5 minutes ago
            ),
            NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Mom",
                content = "Call me when you get home. Love you! 💕",
                fullContent = "Call me when you get home. Love you! 💕",
                timestamp = currentTime - 900000 // 15 minutes ago
            ),
            
            // Gmail notifications (2 notifications)
            NotificationData(
                packageName = "com.google.android.gm",
                appName = "Gmail",
                title = "Weekly Newsletter",
                content = "Your weekly tech digest is here! This week: AI breakthroughs, new programming languages, and mobile development trends.",
                fullContent = "Your weekly tech digest is here! This week: AI breakthroughs, new programming languages, and mobile development trends. Don't miss our exclusive interview with industry leaders about the future of software development.",
                timestamp = currentTime - 1800000 // 30 minutes ago
            ),
            NotificationData(
                packageName = "com.google.android.gm",
                appName = "Gmail",
                title = "Project Update",
                content = "The latest project milestone has been completed. Please review the attached documents and provide feedback.",
                fullContent = "The latest project milestone has been completed. Please review the attached documents and provide feedback by end of week. The team has made significant progress on the core features.",
                timestamp = currentTime - 3600000 // 1 hour ago
            ),
            
            // Slack notifications (2 notifications)
            NotificationData(
                packageName = "com.slack",
                appName = "Slack",
                title = "#general",
                content = "@channel: Team lunch today at 12:30 PM in the main conference room. Pizza will be provided!",
                fullContent = "@channel: Team lunch today at 12:30 PM in the main conference room. Pizza will be provided! Please let us know about any dietary restrictions.",
                timestamp = currentTime - 7200000 // 2 hours ago
            ),
            NotificationData(
                packageName = "com.slack",
                appName = "Slack",
                title = "#development",
                content = "Code review needed for PR #123. The new authentication system is ready for testing.",
                fullContent = "Code review needed for PR #123. The new authentication system is ready for testing. Please check the security implementation and performance optimizations.",
                timestamp = currentTime - 10800000 // 3 hours ago
            ),
            
            // Instagram notification (1 notification)
            NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "New followers",
                content = "alex_photographer and 5 others started following you. Check out their profiles!",
                fullContent = "alex_photographer and 5 others started following you. Check out their profiles and discover new content!",
                timestamp = currentTime - 14400000 // 4 hours ago
            ),
            
            // YouTube notification (1 notification)
            NotificationData(
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                title = "New video from TechChannel",
                content = "\"10 Android Development Tips That Will Change Your Life\" - Watch the latest tutorial now!",
                fullContent = "\"10 Android Development Tips That Will Change Your Life\" - Watch the latest tutorial now! Learn advanced techniques for building better apps.",
                timestamp = currentTime - 18000000 // 5 hours ago
            )
        )
        
        // Add all test notifications
        testNotifications.forEach { notification ->
            notificationAdapter.addNotification(notification)
        }
        
        Toast.makeText(this, "Added ${testNotifications.size} test notifications from ${testNotifications.map { it.appName }.distinct().size} different apps", Toast.LENGTH_SHORT).show()
    }
    
    override fun onItemClick(packageName: String, appName: String) {
         // Launch detail activity for the selected app
         val intent = Intent(this, NotificationDetailActivity::class.java).apply {
             putExtra("package_name", packageName)
             putExtra("app_name", appName)
         }
         detailActivityLauncher.launch(intent)
     }

}