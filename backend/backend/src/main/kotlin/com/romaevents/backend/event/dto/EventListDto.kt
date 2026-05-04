package com.romaevents.backend.event.dto

import java.time.LocalDateTime

//Evento per la lista degli eventi, con meno dettagli rispetto a EventDetailDto, per visualizzare solo le informazioni essenziali nella lista degli eventi

data class EventListDto(
    val id: Long,
    val title: String,
    val category: String?,
    val address: String?,
    val nextOccurrence: LocalDateTime?,
    val status: String
)