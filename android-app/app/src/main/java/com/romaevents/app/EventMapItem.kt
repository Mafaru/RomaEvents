package com.romaevents.app

import kotlinx.serialization.Serializable

@Serializable
data class EventMapItem(
    val id: Long,
    val title: String,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double? = null,
    val nextOccurrenceStart: String? = null,
    val nextOccurrenceEnd: String? = null,
    val status: String? = null
)