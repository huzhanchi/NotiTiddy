package com.example.notitiddy

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

class NotificationDetailAdapter : RecyclerView.Adapter<NotificationDetailAdapter.NotificationViewHolder>() {
    
    private val notifications = mutableListOf<NotificationData>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    interface OnNotificationClickListener {
        fun onNotificationClicked(notificationId: Long)
    }
    
    private var onNotificationClickListener: OnNotificationClickListener? = null
    
    fun setOnNotificationClickListener(listener: OnNotificationClickListener) {
        this.onNotificationClickListener = listener
    }
    
    fun updateNotifications(newNotifications: List<NotificationData>) {
        notifications.clear()
        notifications.addAll(newNotifications)
        notifyDataSetChanged()
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
        private val appIconImageView: ImageView = itemView.findViewById(R.id.appIconImageView)
        private val appNameTextView: TextView = itemView.findViewById(R.id.appNameTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val contentTextView: TextView = itemView.findViewById(R.id.contentTextView)
        private val packageNameTextView: TextView = itemView.findViewById(R.id.packageNameTextView)
        private val fullContentTextView: TextView = itemView.findViewById(R.id.fullContentTextView)
        
        private var isExpanded = false
        
        fun bind(notification: NotificationData) {
            // Hide app icon and app name since they're already shown in the group header
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
                if (notification.content.length > 100) {
                    contentTextView.text = (notification.content ?: "").take(100) + "..."
                    setupContentClickListener(notification)
                } else {
                    contentTextView.text = notification.content
                    contentTextView.setOnClickListener(null)
                }
                contentTextView.visibility = View.VISIBLE
            } else {
                contentTextView.visibility = View.GONE
            }
            
            // Set click listener for the entire item
            itemView.setOnClickListener {
                // Mark notification as read when clicked
                onNotificationClickListener?.onNotificationClicked(notification.id)
                showNotificationDetailDialog(notification)
            }
            
            // Hide package name and full content in detail view
            packageNameTextView.visibility = View.GONE
            fullContentTextView.visibility = View.GONE
        }
        
        private fun setupContentClickListener(notification: NotificationData) {
            contentTextView.setOnClickListener {
                isExpanded = !isExpanded
                if (isExpanded) {
                    contentTextView.text = notification.content ?: ""
                } else {
                    contentTextView.text = (notification.content ?: "").take(100) + "..."
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

            
            // Set dialog content
            dialogTimestampTextView.text = dateFormat.format(notification.date)
            dialogTitleTextView.text = notification.title ?: "No Title"
            
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
                
                val appInfo = packageManager.getApplicationInfo(notification.packageName, 0)
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                dialogAppNameTextView.text = appName
            } catch (e: PackageManager.NameNotFoundException) {
                appIconImageView.setImageResource(android.R.drawable.sym_def_app_icon)
                dialogAppNameTextView.text = notification.packageName
            }
            
            // Set up action listeners
            openActionLayout.setOnClickListener {
                openApp(context, notification)
            }
            
            copyActionLayout.setOnClickListener {
                copyToClipboard(context, notification)
            }
            
            appInfoActionLayout.setOnClickListener {
                openAppInfo(context, notification.packageName)
            }
            
            // Long press for debug info
            dialogView.setOnLongClickListener {
                showDebugInfo(context, notification)
                true
            }
            
            AlertDialog.Builder(context)
                .setView(dialogView)
                .setNegativeButton("Close", null)
                .show()
        }
        
        private fun openApp(context: Context, notification: NotificationData) {
            try {
                if (notification.contentIntent != null) {
                    notification.contentIntent.send()
                } else {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(notification.packageName)
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                    } else {
                        Toast.makeText(context, "Cannot open this app", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to open app: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        private fun copyToClipboard(context: Context, notification: NotificationData) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val contentToCopy = if (!notification.fullContent.isNullOrBlank()) {
                notification.fullContent
            } else {
                notification.content ?: ""
            }
            val clip = ClipData.newPlainText("Notification Content", contentToCopy)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Content copied to clipboard", Toast.LENGTH_SHORT).show()
        }
        
        private fun openAppInfo(context: Context, packageName: String) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            context.startActivity(intent)
        }
        
        private fun showDebugInfo(context: Context, notification: NotificationData) {
            val debugInfo = buildString {
                append("Package: ${notification.packageName}\n")
                append("Title: ${notification.title ?: "N/A"}\n")
                append("Content: ${notification.content ?: "N/A"}\n")
                append("Full Content: ${notification.fullContent ?: "N/A"}\n")
                append("Date: ${dateFormat.format(notification.date)}\n")
                append("Has ContentIntent: ${notification.contentIntent != null}")
            }
            
            AlertDialog.Builder(context)
                .setTitle("Debug Information")
                .setMessage(debugInfo)
                .setPositiveButton("OK", null)
                .show()
        }
    }
}