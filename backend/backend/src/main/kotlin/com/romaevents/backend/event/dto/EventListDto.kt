package com.romaevents.backend.event.dto

import java.time.LocalDateTime

//EventListDto is a simple data transfer object used to represent the basic information of an event when listing events. It includes the event's ID, title, category, address, the date and time of the next occurrence, and the status of the event (e.g., "upcoming", "past", "cancelled"). This DTO is typically used in API responses when fetching a list of events.
data class EventListDto(
    val id: Long,
    val title: String,
    val category: String?,
    val address: String?,
    val nextOccurrence: LocalDateTime?,
    val status: String
)