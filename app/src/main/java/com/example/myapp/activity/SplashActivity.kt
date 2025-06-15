package com.example.myapp.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.myapp.R
import com.example.myapp.process.RetrofitClient

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private val splashTimeout: Long = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.init(applicationContext)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginStatus()
        }, splashTimeout)
    }

    private fun checkLoginStatus() {
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("access_token", null)

        val intent = if (!token.isNullOrEmpty()) {
            Intent(this, HomeActivity::class.java)
        } else {
            Intent(this, SignInActivity::class.java)
        }

        startActivity(intent)
        finish()
    }
}
