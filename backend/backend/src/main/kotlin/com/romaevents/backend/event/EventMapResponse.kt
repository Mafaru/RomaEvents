package com.romaevents.backend.event

import java.time.LocalDateTime

//EventMapResponse is a data transfer object used to represent the information of an event that is relevant for displaying it on a map. It includes the event's ID, title, address, geographical coordinates (latitude and longitude), the distance from the user's location in kilometers, the start and end date and time of the next occurrence, and the status of the event (e.g., "upcoming", "past", "cancelled"). This DTO is typically used in API responses when fetching events to be displayed on a map interface.
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