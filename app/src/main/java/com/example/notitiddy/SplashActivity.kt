package com.example.notitiddy

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashActivity : AppCompatActivity() {
    
    private val splashTimeOut: Long = 2000 // 2 seconds
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splash_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Start logo animation
        val logoImageView = findViewById<ImageView>(R.id.logoImageView)
        val animation = AnimationUtils.loadAnimation(this, R.anim.logo_animation)
        logoImageView.startAnimation(animation)
        
        // Navigate to permission activity after splash timeout
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, PermissionActivity::class.java))
            finish()
        }, splashTimeOut)
    }
}