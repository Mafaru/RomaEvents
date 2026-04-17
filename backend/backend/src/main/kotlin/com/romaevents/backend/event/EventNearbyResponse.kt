package com.romaevents.backend.event

data class EventNearbyResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val distanceKm: Double
)