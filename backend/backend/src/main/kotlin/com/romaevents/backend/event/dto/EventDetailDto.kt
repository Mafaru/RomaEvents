package com.romaevents.backend.event.dto


import java.time.LocalDateTime

//EventDetailDto is a data transfer object used to represent the detailed information of an event. It includes the event's ID, title, description, category, address, geographical coordinates (latitude and longitude), the start and end date and time of the next occurrence, and the status of the event (e.g., "upcoming", "past", "cancelled"). This DTO is typically used in API responses when fetching the details of a specific event.
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