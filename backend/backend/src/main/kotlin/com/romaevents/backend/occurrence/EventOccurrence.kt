package com.romaevents.backend.occurrence

import com.romaevents.backend.event.Event
import jakarta.persistence.*
import java.time.LocalDateTime

//Event occurrence is the actual instance of an event, with a specific date and time. An event can have multiple occurrences (e.g., a concert that happens on multiple days).
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