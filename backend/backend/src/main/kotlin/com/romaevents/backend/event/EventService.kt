package com.romaevents.backend.event

import com.romaevents.backend.event.dto.EventDetailDto
import com.romaevents.backend.event.dto.EventListDto
import com.romaevents.backend.occurrence.EventOccurrenceRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class EventService(
    private val eventRepository: EventRepository,
    private val eventOccurrenceRepository: EventOccurrenceRepository
) {

    fun getAllEventsForList(): List<EventListDto> {
        val now = LocalDateTime.now()
        val occurrences = eventOccurrenceRepository.findUpcoming(now)

        return occurrences.map { eo ->
            val event = eo.event
            val end = eo.endDatetime

            EventListDto(
                id = event.id,
                title = event.title,
                category = event.category?.name,
                address = event.address,
                nextOccurrence = eo.startDatetime,
                status = if (
                    end != null &&
                    (eo.startDatetime.isBefore(now) || eo.startDatetime.isEqual(now)) &&
                    end.isAfter(now)
                ) {
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
            val end = eo.endDatetime

            EventListDto(
                id = event.id,
                title = event.title,
                category = event.category?.name,
                address = event.address,
                nextOccurrence = eo.startDatetime,
                status = if (
                    end != null &&
                    (eo.startDatetime.isBefore(now) || eo.startDatetime.isEqual(now)) &&
                    end.isAfter(now)
                ) {
                    "IN_CORSO"
                } else {
                    "PROSSIMO"
                }
            )
        }
    }

    fun getEventsForListByCategory(categoryId: Long): List<EventListDto> {
        val now = LocalDateTime.now()
        val events = eventRepository.findByCategoryId(categoryId)
        val eventIds = events.map { it.id }.toSet()
        val occurrences = eventOccurrenceRepository.findUpcoming(now)

        return occurrences
            .filter { it.event.id in eventIds }
            .map { eo ->
            val event = eo.event
            val end = eo.endDatetime

             EventListDto(
                id = event.id,
                title = event.title,
                category = event.category?.name,
                address = event.address,
                nextOccurrence = eo.startDatetime,
                status = if (
                    end != null &&
                    (eo.startDatetime.isBefore(now) || eo.startDatetime.isEqual(now)) &&
                    end.isAfter(now)
                ) {
                    "IN_CORSO"
                } else {
                    "PROSSIMO"
                }
            )
        }
}

    fun getEventById(id: Long): EventDetailDto {
        val now = LocalDateTime.now()

        val event = eventRepository.findById(id)
            .orElseThrow { RuntimeException("Evento non trovato") }

        val occurrences = eventOccurrenceRepository.findByEventId(id)

        val active = occurrences.firstOrNull { eo ->
            val end = eo.endDatetime
            end != null &&
            (eo.startDatetime.isBefore(now) || eo.startDatetime.isEqual(now)) &&
            end.isAfter(now)
        }

        val nextUpcoming = occurrences
            .filter { it.startDatetime.isAfter(now) || it.startDatetime.isEqual(now) }
            .minByOrNull { it.startDatetime }

        val chosen = active ?: nextUpcoming ?: occurrences.minByOrNull { it.startDatetime }

        val status = when {
            active != null -> "IN_CORSO"
            nextUpcoming != null -> "PROSSIMO"
            else -> "PASSATO"
        }

        return EventDetailDto(
            id = event.id,
            title = event.title,
            description = event.description,
            category = event.category?.name,
            address = event.address,
            latitude = event.latitude,
            longitude = event.longitude,
            nextOccurrenceStart = chosen?.startDatetime,
            nextOccurrenceEnd = chosen?.endDatetime,
            status = status
        )
    }
}