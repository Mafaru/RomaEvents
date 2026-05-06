package com.romaevents.backend.auth

data class AuthResponse(
    val userId: Long,
    val username: String,
    val email: String,
    val token: String
)