package com.romaevents.app

import kotlinx.serialization.Serializable

@Serializable
data class EventDetail(
    val id: Long,
    val title: String,
    val description: String? = null,
    val rawDateText: String? = null,
    val address: String? = null,
    val category: String? = null,
    val sourceUrl: String? = null
)