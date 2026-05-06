package com.romaevents.app

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val userId: Long,
    val username: String,
    val email: String,
    val token: String
)