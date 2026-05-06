package com.romaevents.app

import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    val temperature: Double,
    val description: String,
    val humidity: Int,
    val windSpeed: Double
)