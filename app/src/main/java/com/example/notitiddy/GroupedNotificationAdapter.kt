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
}

class GroupedNotificationAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val notifications = mutableListOf<NotificationData>()
    private val filteredNotifications = mutableListOf<NotificationData>()
    private val groupedItems = mutableListOf<GroupedItem>()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private var currentFilter = ""
    
    interface OnItemClickListener {
        fun onItemClick(packageName: String, appName: String)
    }
    
    private var onItemClickListener: OnItemClickListener? = null
    
    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }
    
    fun refreshData() {
        rebuildGroupedItems()
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
    }

    fun addNotification(notification: NotificationData) {
        android.util.Log.d("GroupedAdapter", "Adding notification: ${notification.packageName} - ${notification.appName} - ${notification.title}")
        notifications.add(0, notification)
        applyCurrentFilter()
        rebuildGroupedItems()
        notifyDataSetChanged()
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
        
        // Create grouped items - only headers since we removed expand/collapse
        for ((packageName, notificationList) in sortedGroups) {
            val appName = notificationList.first().appName
            val lastNotificationTime = notificationList.maxOf { it.timestamp }
            // Count only unread notifications for the bubble
            val unreadCount = notificationList.count { !it.isRead }
            val header = GroupedItem.AppHeader(packageName, appName, unreadCount, lastNotificationTime)
            groupedItems.add(header)
        }
        
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE_HEADER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_header, parent, false)
        return AppHeaderViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = groupedItems[position] as GroupedItem.AppHeader
        (holder as AppHeaderViewHolder).bind(item)
    }

    override fun getItemCount(): Int = groupedItems.size

    inner class AppHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appIconImageView: ImageView = itemView.findViewById(R.id.headerAppIconImageView)
        private val appNameTextView: TextView = itemView.findViewById(R.id.headerAppNameTextView)
        private val notificationCountTextView: TextView = itemView.findViewById(R.id.headerNotificationCountTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.headerTimestampTextView)

        fun bind(appHeader: GroupedItem.AppHeader) {
            appNameTextView.text = appHeader.appName
            // Display count in bubble, max 99
            val displayCount = if (appHeader.notificationCount > 99) "99" else appHeader.notificationCount.toString()
            notificationCountTextView.text = displayCount
            timestampTextView.text = "Last: ${dateFormat.format(appHeader.lastNotificationTime)}"
            
            // Load app icon
            try {
                val packageManager = itemView.context.packageManager
                val appIcon = packageManager.getApplicationIcon(appHeader.packageName)
                appIconImageView.setImageDrawable(appIcon)
            } catch (e: PackageManager.NameNotFoundException) {
                appIconImageView.setImageResource(android.R.drawable.sym_def_app_icon)
            }
            
            // Set click listener to navigate to detail activity
            itemView.setOnClickListener {
                android.util.Log.d("GroupedAdapter", "Clicking header for package: ${appHeader.packageName}, app: ${appHeader.appName}")
                onItemClickListener?.onItemClick(appHeader.packageName, appHeader.appName)
            }
        }
    }


}