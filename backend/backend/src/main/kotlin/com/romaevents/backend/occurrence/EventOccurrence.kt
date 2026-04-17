package com.romaevents.backend.occurrence

import com.romaevents.backend.event.Event
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "event_occurrences")
data class EventOccurrence(

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