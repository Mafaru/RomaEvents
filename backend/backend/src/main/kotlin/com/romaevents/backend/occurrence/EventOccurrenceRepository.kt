package com.romaevents.backend.occurrence

import org.springframework.data.jpa.repository.JpaRepository

interface EventOccurrenceRepository : JpaRepository<EventOccurrence, Long> {

    fun findByEventId(eventId: Long): List<EventOccurrence>
}