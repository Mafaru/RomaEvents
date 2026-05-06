package com.romaevents.backend.weather

data class WeatherResponse(
    val temperature: Double,
    val description: String,
    val humidity: Int,
    val windSpeed: Double
)