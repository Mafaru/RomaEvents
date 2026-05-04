package com.romaevents.app

import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val id: Long,
    val title: String,
    val category: String? = null,
    val address: String? = null,
    val nextOccurrence: String? = null,
    val status: String? = null
)