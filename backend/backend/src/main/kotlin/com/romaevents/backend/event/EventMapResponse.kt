package com.romaevents.backend.event

import java.time.LocalDateTime

//EventMapResponse è la classe che rappresenta la risposta che viene inviata al frontend quando si richiede la lista degli eventi con i filtri di distanza e data. Contiene solo i campi necessari per visualizzare gli eventi sulla mappa, come id, titolo, indirizzo, latitudine, longitudine, distanza in km, data e ora del prossimo evento (se presente) e lo stato dell'evento (attivo o passato).
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