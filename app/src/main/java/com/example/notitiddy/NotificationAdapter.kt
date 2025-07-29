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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
        private val appIconImageView: ImageView = itemView.findViewById(R.id.appIconImageView)
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
            
            // Load app icon
            try {
                val packageManager = itemView.context.packageManager
                val appIcon = packageManager.getApplicationIcon(notification.packageName)
                appIconImageView.setImageDrawable(appIcon)
            } catch (e: PackageManager.NameNotFoundException) {
                // Use default icon if app not found
                appIconImageView.setImageResource(android.R.drawable.sym_def_app_icon)
            }
            
            // Show/hide and set title
            if (!notification.title.isNullOrEmpty()) {
                titleTextView.text = notification.title
                titleTextView.visibility = View.VISIBLE
            } else {
                titleTextView.visibility = View.GONE
            }
            
            // Show/hide and set content
            if (!notification.content.isNullOrEmpty()) {
                // Show expand button only if there's more content to show
                val hasMoreContent = !notification.fullContent.isNullOrBlank() && 
                    notification.fullContent != notification.content
                
                if (hasMoreContent) {
                    // Show truncated content with clickable "...more" at the end
                    contentTextView.text = "${notification.content}...more"
                    setupContentClickListener(notification)
                    expandButton.visibility = View.GONE
                    adjustContentConstraints(false)
                } else {
                    contentTextView.text = notification.content
                    contentTextView.setOnClickListener(null)
                    expandButton.visibility = View.GONE
                    adjustContentConstraints(false)
                }
                contentTextView.visibility = View.VISIBLE
            } else {
                contentTextView.visibility = View.GONE
                expandButton.visibility = View.GONE
                adjustContentConstraints(false)
            }
            
            // Reset expansion state
            isExpanded = false
            fullContentTextView.visibility = View.GONE
            expandButton.text = "Show more"
            
            // Package name with status
            val status = if (notification.isRemoved) "[REMOVED] " else ""
            packageNameTextView.text = "$status${notification.packageName}"
            
            // Set click listener for the entire item
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
        
        private fun setupExpandButton(notification: NotificationData) {
            expandButton.setOnClickListener {
                isExpanded = !isExpanded
                if (isExpanded) {
                    fullContentTextView.text = notification.fullContent
                    fullContentTextView.visibility = View.VISIBLE
                    expandButton.text = "Show less"
                } else {
                    fullContentTextView.visibility = View.GONE
                    expandButton.text = "Show more"
                }
            }
        }
        
        private fun adjustContentConstraints(hasExpandButton: Boolean) {
            val layoutParams = contentTextView.layoutParams as ConstraintLayout.LayoutParams
            if (hasExpandButton) {
                layoutParams.endToEnd = ConstraintLayout.LayoutParams.UNSET
                layoutParams.endToStart = R.id.expandButton
                // Convert 8dp to pixels
                val density = itemView.context.resources.displayMetrics.density
                layoutParams.marginEnd = (8 * density).toInt()
            } else {
                layoutParams.endToStart = ConstraintLayout.LayoutParams.UNSET
                layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                layoutParams.marginEnd = 0
            }
            contentTextView.layoutParams = layoutParams
        }
        
        private fun showNotificationDetailDialog(notification: NotificationData) {
            val context = itemView.context
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_notification_detail, null)
            
            // Set up dialog views
            val appIconImageView = dialogView.findViewById<ImageView>(R.id.appIconImageView)
            val dialogAppNameTextView = dialogView.findViewById<TextView>(R.id.dialogAppNameTextView)
            val dialogTimestampTextView = dialogView.findViewById<TextView>(R.id.dialogTimestampTextView)
            val dialogTitleTextView = dialogView.findViewById<TextView>(R.id.dialogTitleTextView)
            val dialogContentTextView = dialogView.findViewById<TextView>(R.id.dialogContentTextView)
            val openActionLayout = dialogView.findViewById<LinearLayout>(R.id.openActionLayout)
            val copyActionLayout = dialogView.findViewById<LinearLayout>(R.id.copyActionLayout)
            val appInfoActionLayout = dialogView.findViewById<LinearLayout>(R.id.appInfoActionLayout)
            
            // Populate dialog with notification data
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
            
            // Try to get app icon
            try {
                val packageManager = context.packageManager
                val appIcon = packageManager.getApplicationIcon(notification.packageName)
                appIconImageView.setImageDrawable(appIcon)
            } catch (e: PackageManager.NameNotFoundException) {
                // Use default icon if app not found
                appIconImageView.setImageResource(android.R.drawable.sym_def_app_icon)
            }
            
            // Create and show dialog
            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .create()
            
            // Set up action button click listeners
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
            
            // Add long press for debug info
            dialogView.setOnLongClickListener {
                showDebugInfo(context, notification)
                true
            }
            
            dialog.show()
        }
        
        private fun openApp(context: Context, notification: NotificationData) {
            try {
                // Try to use the original notification's content intent first
                if (notification.contentIntent != null) {
                    try {
                        // Add timeout handling for PendingIntent
                        notification.contentIntent.send(context, 0, null, null, null)
                        
                        // Show feedback that intent was sent
                        Toast.makeText(context, "Opening notification content...", Toast.LENGTH_SHORT).show()
                        return
                    } catch (e: PendingIntent.CanceledException) {
                        Toast.makeText(context, "Notification content expired, opening app instead", Toast.LENGTH_LONG).show()
                        // Fall through to normal app launch
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to open notification content: ${e.message}", Toast.LENGTH_LONG).show()
                        // Fall through to normal app launch
                    }
                }
                
                // Fallback to launching the app normally
                val intent = context.packageManager.getLaunchIntentForPackage(notification.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    context.startActivity(intent)
                    Toast.makeText(context, "Opening ${notification.appName}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "App ${notification.appName} not found or uninstalled", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error opening app: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        
        private fun copyToClipboard(context: Context, notification: NotificationData) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val contentToCopy = buildString {
                append("App: ${notification.appName}\n")
                if (!notification.title.isNullOrEmpty()) {
                    append("Title: ${notification.title}\n")
                }
                val content = if (!notification.fullContent.isNullOrBlank()) {
                    notification.fullContent
                } else {
                    notification.content
                }
                append("Content: $content\n")
                append("Time: ${dateFormat.format(notification.date)}")
            }
            
            val clip = ClipData.newPlainText("Notification", contentToCopy)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
        
        private fun openAppInfo(context: Context, packageName: String) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot open app info", Toast.LENGTH_SHORT).show()
            }
        }
        
        private fun showDebugInfo(context: Context, notification: NotificationData) {
            val debugInfo = buildString {
                append("=== NOTIFICATION DEBUG INFO ===\n\n")
                append("Package: ${notification.packageName}\n")
                append("App Name: ${notification.appName}\n")
                append("Timestamp: ${notification.timestamp}\n")
                append("Date: ${dateFormat.format(notification.date)}\n\n")
                
                append("=== CONTENT INTENT ===\n")
                if (notification.contentIntent != null) {
                    append("Content Intent: Available\n")
                    append("Intent Details: ${notification.contentIntent}\n")
                    try {
                        append("Creator Package: ${notification.contentIntent.creatorPackage}\n")
                        append("Creator UID: ${notification.contentIntent.creatorUid}\n")
                    } catch (e: Exception) {
                        append("Intent Info Error: ${e.message}\n")
                    }
                } else {
                    append("Content Intent: NULL (No deep link available)\n")
                }
                
                append("\n=== APP STATUS ===\n")
                try {
                    val pm = context.packageManager
                    val appInfo = pm.getApplicationInfo(notification.packageName, 0)
                    append("App Installed: YES\n")
                    append("App Enabled: ${appInfo.enabled}\n")
                    
                    val launchIntent = pm.getLaunchIntentForPackage(notification.packageName)
                    append("Launch Intent: ${if (launchIntent != null) "Available" else "NULL"}\n")
                } catch (e: PackageManager.NameNotFoundException) {
                    append("App Installed: NO (Uninstalled)\n")
                } catch (e: Exception) {
                    append("App Status Error: ${e.message}\n")
                }
                
                append("\n=== TROUBLESHOOTING TIPS ===\n")
                append("• If Content Intent is NULL: App doesn't support deep linking\n")
                append("• If Intent hangs: App may have security restrictions\n")
                append("• If App Uninstalled: Notification is from removed app\n")
                append("• Try force-stopping the target app and retry\n")
            }
            
            AlertDialog.Builder(context)
                .setTitle("Debug Information")
                .setMessage(debugInfo)
                .setPositiveButton("Copy to Clipboard") { _, _ ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Debug Info", debugInfo)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Debug info copied to clipboard", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }
}