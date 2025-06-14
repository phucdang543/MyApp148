package com.example.myapp.process.login


data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String
)

data class RegisterRequest(
    val name: String,
    val username: String,
    val password: String,
    val repeatpassword: String
)

data class LogoutResponse(
    val message: String
)

