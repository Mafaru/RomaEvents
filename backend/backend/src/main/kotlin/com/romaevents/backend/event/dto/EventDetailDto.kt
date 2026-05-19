package com.romaevents.backend.event.dto


import java.time.LocalDateTime

data class EventDetailDto(
    val id: Long,
    val title: String,
    val description: String?,
    val category: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val nextOccurrenceStart: LocalDateTime?,
    val nextOccurrenceEnd: LocalDateTime?,
    val status: String
)