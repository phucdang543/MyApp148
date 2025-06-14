package com.example.myapp.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapp.R
import com.example.myapp.databinding.ActivitySignInBinding
import com.example.myapp.process.RetrofitClient
import com.example.myapp.process.login.LoginRequest
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
    private lateinit var btnLogin: Button
    private lateinit var tvSignUp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initViews()
        setupViews()
    }

    private fun initViews() {
        btnLogin = binding.btnLogin
        tvSignUp = binding.tvSignUp
    }

    @SuppressLint("UseKtx")
    private fun setupViews() {
        btnLogin.setOnClickListener {
            val username = binding.edtUsername.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Snackbar.make(
                    btnLogin,
                    getString(R.string.email_password_required),
                    Snackbar.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.authService.login(LoginRequest(username, password))
                    val token = response.token
                    val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
                    sharedPreferences.edit().putString("access_token", token).apply()

                    val intent = Intent(this@SignInActivity, HomeActivity::class.java)
                    startActivity(intent)
                    finish()

                } catch (e: Exception) {
                    e.printStackTrace()
                    Snackbar.make(btnLogin, getString(R.string.login_failed), Snackbar.LENGTH_SHORT)
                        .show()
                }
            }
        }
        tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }
}






