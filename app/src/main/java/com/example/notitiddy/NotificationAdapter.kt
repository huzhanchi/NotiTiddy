package com.example.notitiddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationAdapter : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private val notifications = mutableListOf<NotificationData>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun addNotification(notification: NotificationData) {
        notifications.add(0, notification) // Add to the beginning of the list
        notifyItemInserted(0)
    }

    fun updateNotificationStatus(packageName: String) {
        val index = notifications.indexOfFirst { it.packageName == packageName }
        if (index != -1) {
            val notification = notifications[index]
            notifications[index] = notification.copy(isRemoved = true)
            notifyItemChanged(index)
        }
    }

    fun clearNotifications() {
        val size = notifications.size
        notifications.clear()
        notifyItemRangeRemoved(0, size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appNameTextView: TextView = itemView.findViewById(R.id.appNameTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val contentTextView: TextView = itemView.findViewById(R.id.contentTextView)
        private val packageNameTextView: TextView = itemView.findViewById(R.id.packageNameTextView)
        private val fullContentTextView: TextView = itemView.findViewById(R.id.fullContentTextView)
        private val expandButton: Button = itemView.findViewById(R.id.expandButton)
        
        private var isExpanded = false

        fun bind(notification: NotificationData) {
            appNameTextView.text = notification.appName
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
                contentTextView.text = notification.content
                contentTextView.visibility = View.VISIBLE
                
                // Show expand button only if there's more content to show
                val hasMoreContent = !notification.fullContent.isNullOrBlank() && 
                    notification.fullContent != notification.content
                if (hasMoreContent) {
                    expandButton.visibility = View.VISIBLE
                    setupExpandButton(notification)
                    // Adjust contentTextView constraints to account for expand button
                    adjustContentConstraints(true)
                } else {
                    expandButton.visibility = View.GONE
                    // Adjust contentTextView constraints to span full width
                    adjustContentConstraints(false)
                }
            } else {
                contentTextView.visibility = View.GONE
                expandButton.visibility = View.GONE
                adjustContentConstraints(false)
            }
            
            // Reset expansion state
            isExpanded = false
            fullContentTextView.visibility = View.GONE
            expandButton.text = "▼"
            
            // Package name with status
            val status = if (notification.isRemoved) "[REMOVED] " else ""
            packageNameTextView.text = "$status${notification.packageName}"
        }
        
        private fun setupExpandButton(notification: NotificationData) {
            expandButton.setOnClickListener {
                isExpanded = !isExpanded
                if (isExpanded) {
                    fullContentTextView.text = notification.fullContent
                    fullContentTextView.visibility = View.VISIBLE
                    expandButton.text = "▲"
                } else {
                    fullContentTextView.visibility = View.GONE
                    expandButton.text = "▼"
                }
            }
        }
        
        private fun adjustContentConstraints(hasExpandButton: Boolean) {
            val layoutParams = contentTextView.layoutParams as ConstraintLayout.LayoutParams
            if (hasExpandButton) {
                layoutParams.endToEnd = ConstraintLayout.LayoutParams.UNSET
                layoutParams.endToStart = R.id.expandButton
                layoutParams.marginEnd = 24 // 8dp margin in pixels (approximately)
            } else {
                layoutParams.endToStart = ConstraintLayout.LayoutParams.UNSET
                layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                layoutParams.marginEnd = 0
            }
            contentTextView.layoutParams = layoutParams
        }
    }
}