package com.romaevents.app

import kotlinx.serialization.Serializable

@Serializable
data class EventDetail(
    val id: Long,
    val title: String,
    val description: String? = null,
    val category: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val nextOccurrenceStart: String? = null,
    val nextOccurrenceEnd: String? = null,
    val status: String? = null
)