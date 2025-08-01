package com.example.notitiddy

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PermissionActivity : AppCompatActivity() {
    
    private lateinit var grantPermissionButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.permission_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        grantPermissionButton = findViewById(R.id.grantPermissionButton)
        
        grantPermissionButton.setOnClickListener {
            if (!isNotificationServiceEnabled()) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            } else {
                navigateToMainActivity()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Check if permission is already granted
        if (isNotificationServiceEnabled()) {
            navigateToMainActivity()
        }
    }
    
    private fun isNotificationServiceEnabled(): Boolean {
        val packageName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":")
            for (name in names) {
                val componentName = android.content.ComponentName.unflattenFromString(name)
                if (componentName != null) {
                    if (TextUtils.equals(packageName, componentName.packageName)) {
                        return true
                    }
                }
            }
        }
        return false
    }
    
    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}