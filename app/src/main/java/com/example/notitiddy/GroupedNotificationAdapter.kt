package com.example.notitiddy

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.provider.Settings
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

sealed class GroupedItem {
    data class AppHeader(
        val packageName: String,
        val appName: String,
        val notificationCount: Int,
        val lastNotificationTime: Long
    ) : GroupedItem()
    
    data class NotificationItem(
        val notification: NotificationData
    ) : GroupedItem()
}

class GroupedNotificationAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val notifications = mutableListOf<NotificationData>()
    private val filteredNotifications = mutableListOf<NotificationData>()
    private val groupedItems = mutableListOf<GroupedItem>()
    private val expandedGroups = mutableSetOf<String>() // Track expanded groups by package name
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private var currentFilter = ""

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_NOTIFICATION = 1
    }

    fun addNotification(notification: NotificationData) {
        notifications.add(0, notification)
        applyCurrentFilter()
        rebuildGroupedItems()
    }

    fun updateNotificationStatus(packageName: String) {
        val index = notifications.indexOfFirst { it.packageName == packageName }
        if (index != -1) {
            val notification = notifications[index]
            notifications[index] = notification.copy(isRemoved = true)
            rebuildGroupedItems()
        }
    }

    fun clearNotifications() {
        notifications.clear()
        filteredNotifications.clear()
        groupedItems.clear()
        notifyDataSetChanged()
    }
    
    fun filter(query: String) {
        currentFilter = query.lowercase().trim()
        applyCurrentFilter()
        rebuildGroupedItems()
    }
    
    private fun applyCurrentFilter() {
        filteredNotifications.clear()
        if (currentFilter.isEmpty()) {
            filteredNotifications.addAll(notifications)
        } else {
            filteredNotifications.addAll(notifications.filter { notification ->
                notification.appName.lowercase().contains(currentFilter) ||
                notification.title?.lowercase()?.contains(currentFilter) == true ||
                notification.content?.lowercase()?.contains(currentFilter) == true ||
                notification.fullContent?.lowercase()?.contains(currentFilter) == true
            })
        }
    }

    private fun rebuildGroupedItems() {
        groupedItems.clear()
        
        // Group filtered notifications by package name
        val groupedNotifications = filteredNotifications.groupBy { it.packageName }
        
        // Sort groups by most recent notification time
        val sortedGroups = groupedNotifications.toList().sortedByDescending { (_, notificationList) ->
            notificationList.maxOf { it.timestamp }
        }
        
        // Create grouped items
        for ((packageName, notificationList) in sortedGroups) {
            val appName = notificationList.first().appName
            val lastNotificationTime = notificationList.maxOf { it.timestamp }
            val header = GroupedItem.AppHeader(packageName, appName, notificationList.size, lastNotificationTime)
            groupedItems.add(header)
            
            // Add notifications for this app only if group is expanded
            if (expandedGroups.contains(packageName)) {
                notificationList.forEach { notification ->
                    groupedItems.add(GroupedItem.NotificationItem(notification))
                }
            }
        }
        
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (groupedItems[position]) {
            is GroupedItem.AppHeader -> VIEW_TYPE_HEADER
            is GroupedItem.NotificationItem -> VIEW_TYPE_NOTIFICATION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_app_header, parent, false)
                AppHeaderViewHolder(view)
            }
            VIEW_TYPE_NOTIFICATION -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_notification, parent, false)
                NotificationViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = groupedItems[position]) {
            is GroupedItem.AppHeader -> {
                (holder as AppHeaderViewHolder).bind(item)
            }
            is GroupedItem.NotificationItem -> {
                (holder as NotificationViewHolder).bind(item.notification)
            }
        }
    }

    override fun getItemCount(): Int = groupedItems.size
    
    private fun toggleGroup(packageName: String) {
        val headerPosition = groupedItems.indexOfFirst { 
            it is GroupedItem.AppHeader && it.packageName == packageName 
        }
        
        if (headerPosition == -1) return
        
        if (expandedGroups.contains(packageName)) {
            // Collapse: Remove notification items with animation
            expandedGroups.remove(packageName)
            val notificationsToRemove = mutableListOf<Int>()
            
            // Find all notification items for this package
            for (i in headerPosition + 1 until groupedItems.size) {
                val item = groupedItems[i]
                if (item is GroupedItem.NotificationItem && 
                    item.notification.packageName == packageName) {
                    notificationsToRemove.add(i)
                } else if (item is GroupedItem.AppHeader) {
                    break // Reached next group
                }
            }
            
            // Remove items in reverse order to maintain correct indices
            notificationsToRemove.reversed().forEach { position ->
                groupedItems.removeAt(position)
                notifyItemRemoved(position)
            }
            
        } else {
            // Expand: Add notification items with animation
            expandedGroups.add(packageName)
            val notificationsForPackage = notifications.filter { it.packageName == packageName }
            
            var insertPosition = headerPosition + 1
            notificationsForPackage.forEach { notification ->
                groupedItems.add(insertPosition, GroupedItem.NotificationItem(notification))
                notifyItemInserted(insertPosition)
                insertPosition++
            }
        }
        
        // Update the header to reflect the new state
        notifyItemChanged(headerPosition)
    }

    inner class AppHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appIconImageView: ImageView = itemView.findViewById(R.id.headerAppIconImageView)
        private val appNameTextView: TextView = itemView.findViewById(R.id.headerAppNameTextView)
        private val notificationCountTextView: TextView = itemView.findViewById(R.id.headerNotificationCountTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.headerTimestampTextView)
        private val arrowImageView: ImageView = itemView.findViewById(R.id.headerArrowImageView)

        fun bind(appHeader: GroupedItem.AppHeader) {
            val isExpanded = expandedGroups.contains(appHeader.packageName)
            
            appNameTextView.text = appHeader.appName
            notificationCountTextView.text = "${appHeader.notificationCount} notification${if (appHeader.notificationCount != 1) "s" else ""}"
            timestampTextView.text = "Last: ${dateFormat.format(appHeader.lastNotificationTime)}"
            
            // Animate arrow rotation
            val targetRotation = if (isExpanded) 0f else -90f
            if (arrowImageView.rotation != targetRotation) {
                val rotateAnimation = RotateAnimation(
                    arrowImageView.rotation,
                    targetRotation,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
                ).apply {
                    duration = 200
                    fillAfter = true
                }
                arrowImageView.startAnimation(rotateAnimation)
                arrowImageView.rotation = targetRotation
            }
            
            // Load app icon
            try {
                val packageManager = itemView.context.packageManager
                val appIcon = packageManager.getApplicationIcon(appHeader.packageName)
                appIconImageView.setImageDrawable(appIcon)
            } catch (e: PackageManager.NameNotFoundException) {
                appIconImageView.setImageResource(android.R.drawable.sym_def_app_icon)
            }
            
            // Set click listener to toggle group expansion
            itemView.setOnClickListener {
                animateArrowAndToggle(appHeader.packageName)
            }
        }
        
        private fun animateArrowAndToggle(packageName: String) {
            val isCurrentlyExpanded = expandedGroups.contains(packageName)
            val targetRotation = if (isCurrentlyExpanded) -90f else 0f
            
            val rotateAnimation = RotateAnimation(
                arrowImageView.rotation,
                targetRotation,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 200
                fillAfter = true
            }
            
            arrowImageView.startAnimation(rotateAnimation)
            arrowImageView.rotation = targetRotation
            
            // Delay the toggle to sync with arrow animation
            itemView.postDelayed({
                toggleGroup(packageName)
            }, 50)
        }
    }

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appIconImageView: ImageView = itemView.findViewById(R.id.appIconImageView)
        private val appNameTextView: TextView = itemView.findViewById(R.id.appNameTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val contentTextView: TextView = itemView.findViewById(R.id.contentTextView)
        private val packageNameTextView: TextView = itemView.findViewById(R.id.packageNameTextView)
        private val fullContentTextView: TextView = itemView.findViewById(R.id.fullContentTextView)
        
        private var isExpanded = false

        fun bind(notification: NotificationData) {
            // Hide app icon and app name since they're shown in the group header
            appIconImageView.visibility = View.GONE
            appNameTextView.visibility = View.GONE
            timestampTextView.text = dateFormat.format(notification.date)
            
            // Show/hide and set title
            if (!notification.title.isNullOrEmpty()) {
                titleTextView.text = notification.title
                titleTextView.visibility = View.VISIBLE
            } else {
                titleTextView.visibility = View.GONE
            }
            
            // Show/hide and set content
            if (!notification.content.isNullOrEmpty()) {
                val hasMoreContent = !notification.fullContent.isNullOrBlank() && 
                    notification.fullContent != notification.content
                
                if (hasMoreContent) {
                    val fullText = "${notification.content}...more"
                    val spannableString = SpannableString(fullText)
                    val moreStartIndex = fullText.length - 7
                    spannableString.setSpan(
                        ForegroundColorSpan(Color.BLUE),
                        moreStartIndex,
                        fullText.length,
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    contentTextView.text = spannableString
                    setupContentClickListener(notification)
                } else {
                    contentTextView.text = notification.content
                    contentTextView.setOnClickListener(null)
                }
                contentTextView.visibility = View.VISIBLE
            } else {
                contentTextView.visibility = View.GONE
            }
            
            isExpanded = false
            fullContentTextView.visibility = View.GONE
            
            val status = if (notification.isRemoved) "[REMOVED] " else ""
            packageNameTextView.text = "$status${notification.packageName}"
            
            itemView.setOnClickListener {
                showNotificationDetailDialog(notification)
            }
        }
        
        private fun setupContentClickListener(notification: NotificationData) {
            contentTextView.setOnClickListener {
                isExpanded = !isExpanded
                if (isExpanded) {
                    contentTextView.text = notification.fullContent
                } else {
                    contentTextView.text = "${notification.content}...more"
                }
            }
        }
        
        private fun showNotificationDetailDialog(notification: NotificationData) {
            val context = itemView.context
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_notification_detail, null)
            
            val appIconImageView = dialogView.findViewById<ImageView>(R.id.appIconImageView)
            val dialogAppNameTextView = dialogView.findViewById<TextView>(R.id.dialogAppNameTextView)
            val dialogTimestampTextView = dialogView.findViewById<TextView>(R.id.dialogTimestampTextView)
            val dialogTitleTextView = dialogView.findViewById<TextView>(R.id.dialogTitleTextView)
            val dialogContentTextView = dialogView.findViewById<TextView>(R.id.dialogContentTextView)
            val openActionLayout = dialogView.findViewById<LinearLayout>(R.id.openActionLayout)
            val copyActionLayout = dialogView.findViewById<LinearLayout>(R.id.copyActionLayout)
            val appInfoActionLayout = dialogView.findViewById<LinearLayout>(R.id.appInfoActionLayout)
            
            dialogAppNameTextView.text = notification.appName
            dialogTimestampTextView.text = dateFormat.format(notification.date)
            
            if (!notification.title.isNullOrEmpty()) {
                dialogTitleTextView.text = notification.title
                dialogTitleTextView.visibility = View.VISIBLE
            } else {
                dialogTitleTextView.visibility = View.GONE
            }
            
            val contentToShow = if (!notification.fullContent.isNullOrBlank()) {
                notification.fullContent
            } else {
                notification.content
            }
            dialogContentTextView.text = contentToShow
            
            try {
                val packageManager = context.packageManager
                val appIcon = packageManager.getApplicationIcon(notification.packageName)
                appIconImageView.setImageDrawable(appIcon)
            } catch (e: PackageManager.NameNotFoundException) {
                appIconImageView.setImageResource(android.R.drawable.sym_def_app_icon)
            }
            
            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .create()
            
            openActionLayout.setOnClickListener {
                openApp(context, notification)
                dialog.dismiss()
            }
            
            copyActionLayout.setOnClickListener {
                copyToClipboard(context, notification)
                dialog.dismiss()
            }
            
            appInfoActionLayout.setOnClickListener {
                openAppInfo(context, notification.packageName)
                dialog.dismiss()
            }
            
            dialogView.setOnLongClickListener {
                showDebugInfo(context, notification)
                true
            }
            
            dialog.show()
        }
        
        private fun openApp(context: Context, notification: NotificationData) {
            try {
                if (notification.contentIntent != null) {
                    Toast.makeText(context, "Attempting to open notification content...", Toast.LENGTH_SHORT).show()
                    
                    notification.contentIntent.send(context, 0, null, object : PendingIntent.OnFinished {
                        override fun onSendFinished(
                            pendingIntent: PendingIntent?,
                            intent: Intent?,
                            resultCode: Int,
                            resultData: String?,
                            resultExtras: android.os.Bundle?
                        ) {
                            val message = when (resultCode) {
                                android.app.Activity.RESULT_OK -> "Successfully opened notification content"
                                android.app.Activity.RESULT_CANCELED -> "Opening notification content was canceled"
                                else -> "Failed to open notification content (Error code: $resultCode)"
                            }
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    }, null)
                } else {
                    val packageManager = context.packageManager
                    val launchIntent = packageManager.getLaunchIntentForPackage(notification.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        context.startActivity(launchIntent)
                        Toast.makeText(context, "Opening ${notification.appName}...", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "App not found: ${notification.appName}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error opening app: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        private fun copyToClipboard(context: Context, notification: NotificationData) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val contentToCopy = if (!notification.fullContent.isNullOrBlank()) {
                "${notification.title}\n${notification.fullContent}"
            } else {
                "${notification.title}\n${notification.content}"
            }
            val clip = ClipData.newPlainText("Notification Content", contentToCopy)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
        
        private fun openAppInfo(context: Context, packageName: String) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            context.startActivity(intent)
        }
        
        private fun showDebugInfo(context: Context, notification: NotificationData) {
            val debugInfo = buildString {
                appendLine("Package: ${notification.packageName}")
                appendLine("App: ${notification.appName}")
                appendLine("Title: ${notification.title}")
                appendLine("Content: ${notification.content}")
                appendLine("Full Content: ${notification.fullContent}")
                appendLine("Timestamp: ${notification.timestamp}")
                appendLine("Content Intent: ${notification.contentIntent}")
                appendLine("Is Removed: ${notification.isRemoved}")
            }
            
            AlertDialog.Builder(context)
                .setTitle("Debug Info")
                .setMessage(debugInfo)
                .setPositiveButton("OK", null)
                .show()
        }
    }
}