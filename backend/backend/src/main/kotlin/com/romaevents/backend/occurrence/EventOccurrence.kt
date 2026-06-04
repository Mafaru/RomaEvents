package com.romaevents.backend.occurrence

import com.romaevents.backend.event.Event
import jakarta.persistence.*
import java.time.LocalDateTime

//EventOccurrence è la classe che rappresenta le singole occorrenze di un evento, con data e ora di inizio e fine. Un evento può avere più occorrenze, ad esempio se si ripete ogni settimana o ogni mese. La classe contiene un riferimento all'evento a cui appartiene l'occorrenza, e i campi startDatetime e endDatetime che indicano quando inizia e quando finisce l'occorrenza.

@Entity
@Table(name = "event_occurrences")
class EventOccurrence(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "event_id")
    val event: Event,

    @Column(nullable = false)
    val startDatetime: LocalDateTime,

    val endDatetime: LocalDateTime?
)