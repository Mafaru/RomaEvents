package com.romaevents.app.model


import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val message: String
)