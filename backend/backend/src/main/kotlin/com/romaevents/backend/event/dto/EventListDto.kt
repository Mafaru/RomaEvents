package com.romaevents.backend.event.dto

import java.time.LocalDateTime

data class EventListDto(
    val id: Long,
    val title: String,
    val category: String?,
    val address: String?,
    val nextOccurrence: LocalDateTime?,
    val status: String
)