package com.romaevents.backend.event

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface EventOccurrenceRepository : JpaRepository<EventOccurrence, Long> {

    @Query("""
        SELECT eo FROM EventOccurrence eo
        WHERE eo.startDatetime >= :now
        ORDER BY eo.startDatetime ASC
    """)
    fun findUpcoming(@Param("now") now: LocalDateTime): List<EventOccurrence>

    @Query("""
        SELECT eo FROM EventOccurrence eo
        WHERE eo.startDatetime BETWEEN :start AND :end
        ORDER BY eo.startDatetime ASC
    """)
    fun findBetween(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): List<EventOccurrence>
}