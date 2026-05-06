package com.romaevents.backend.auth

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)