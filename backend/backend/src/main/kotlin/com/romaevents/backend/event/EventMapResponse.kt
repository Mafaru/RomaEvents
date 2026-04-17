package com.romaevents.backend.event

import java.time.LocalDateTime

data class EventMapResponse(
    val id: Long,
    val title: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val nextOccurrenceStart: LocalDateTime?,
    val nextOccurrenceEnd: LocalDateTime?,
    val status: EventStatus
)