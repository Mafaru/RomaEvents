package com.romaevents.backend.event

import com.romaevents.backend.event.dto.EventListDto
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class EventService(
    private val eventOccurrenceRepository: EventOccurrenceRepository
) {

    fun getAllEventsForList(): List<EventListDto> {

        val now = LocalDateTime.now()

        val occurrences = eventOccurrenceRepository.findUpcoming(now)

        return occurrences.map { eo ->

            val event = eo.event

            EventListDto(
                id = event.id,
                title = event.title,
                category = event.category?.name,
                address = event.address,
                nextOccurrence = eo.startDatetime,
                status = if (eo.startDatetime.isBefore(now)) {
                    "IN_CORSO"
                } else {
                    "PROSSIMO"
                }
            )
        }
    }

    fun getEventsForMap(): List<EventListDto> {

        val now = LocalDateTime.now()
        val oneMonthLater = now.plusMonths(1)

        val occurrences = eventOccurrenceRepository.findBetween(now, oneMonthLater)

        return occurrences.map { eo ->

            val event = eo.event

            EventListDto(
                id = event.id,
                title = event.title,
                category = event.category?.name,
                address = event.address,
                nextOccurrence = eo.startDatetime,
                status = if (eo.startDatetime.isBefore(now)) {
                    "IN_CORSO"
                } else {
                    "PROSSIMO"
                }
            )
        }
    }
}