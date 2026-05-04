package com.romaevents.backend.event.dto


import java.time.LocalDateTime

//oggettoDTO(data transfer object): oggetto mandato fuori tramite api, quindi come vogliamo visualizzarlo fuori dal db evitando campi inutili o troppo complessi da visualizzare
//eventoDetailDto: evento con tutti i dettagli, per visualizzare tutte le informazioni di un evento quando si clicca su di esso nella lista degli eventi


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