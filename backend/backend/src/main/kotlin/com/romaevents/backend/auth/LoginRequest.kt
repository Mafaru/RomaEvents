package com.romaevents.backend.auth

data class LoginRequest(
    val email: String,
    val password: String
)