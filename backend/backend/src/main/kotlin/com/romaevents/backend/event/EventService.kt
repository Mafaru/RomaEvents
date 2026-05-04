package com.romaevents.backend.event

import com.romaevents.backend.event.dto.EventDetailDto
import com.romaevents.backend.event.dto.EventListDto
import com.romaevents.backend.occurrence.EventOccurrence
import com.romaevents.backend.occurrence.EventOccurrenceRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.math.*

@Service
class EventService(
    private val eventRepository: EventRepository,
    private val eventOccurrenceRepository: EventOccurrenceRepository
) {

    fun getAllEventsForList(): List<EventListDto> {
        val now = LocalDateTime.now()

        return eventRepository.findAll()
            .mapNotNull { event ->
                val occurrences = eventOccurrenceRepository.findByEventId(event.id)
                chooseRelevantOccurrence(occurrences, now)?.let { occurrence ->
                    toEventListDto(occurrence, now)
                }
            }
            .sortedWith(
                compareBy<EventListDto> { it.status != "IN_CORSO" }
                    .thenBy { it.nextOccurrence }
            )
    }

    fun getEventsForListByCategory(categoryId: Long): List<EventListDto> {
        val now = LocalDateTime.now()

        return eventRepository.findByCategoryId(categoryId)
            .mapNotNull { event ->
                val occurrences = eventOccurrenceRepository.findByEventId(event.id)
                chooseRelevantOccurrence(occurrences, now)?.let { occurrence ->
                    toEventListDto(occurrence, now)
                }
            }
            .sortedWith(
                compareBy<EventListDto> { it.status != "IN_CORSO" }
                    .thenBy { it.nextOccurrence }
            )
    }

    fun getEventsForMap(): List<EventListDto> {
        return getAllEventsForList()
    }

    fun getMapEvents(
        lat: Double,
        lon: Double,
        radiusKm: Double
    ): List<EventMapResponse> {
        val now = LocalDateTime.now()

        return eventRepository.findAll()
            .asSequence()
            .filter { event ->
                event.latitude != null && event.longitude != null
            }
            .mapNotNull { event ->
                val eventLat = event.latitude ?: return@mapNotNull null
                val eventLon = event.longitude ?: return@mapNotNull null

                val distance = haversine(
                    lat1 = lat,
                    lon1 = lon,
                    lat2 = eventLat,
                    lon2 = eventLon
                )

                if (distance > radiusKm) {
                    return@mapNotNull null
                }

                val occurrences = eventOccurrenceRepository.findByEventId(event.id)
                val chosenOccurrence = chooseRelevantOccurrence(occurrences, now)
                    ?: return@mapNotNull null

                val status = if (isActiveNow(chosenOccurrence, now)) {
                    EventStatus.ACTIVE_NOW
                } else {
                    EventStatus.UPCOMING
                }

                EventMapResponse(
                    id = event.id,
                    title = event.title,
                    address = event.address,
                    latitude = eventLat,
                    longitude = eventLon,
                    distanceKm = round(distance * 100) / 100.0,
                    nextOccurrenceStart = chosenOccurrence.startDatetime,
                    nextOccurrenceEnd = chosenOccurrence.endDatetime,
                    status = status
                )
            }
            .sortedWith(
                compareBy<EventMapResponse> { it.status != EventStatus.ACTIVE_NOW }
                    .thenBy { it.distanceKm }
                    .thenBy { it.nextOccurrenceStart }
            )
            .toList()
    }

    fun getEventById(id: Long): EventDetailDto {
        val now = LocalDateTime.now()

        val event = eventRepository.findById(id)
            .orElseThrow { RuntimeException("Evento non trovato") }

        val occurrences = eventOccurrenceRepository.findByEventId(id)

        val chosen = chooseRelevantOccurrence(occurrences, now)
            ?: occurrences.minByOrNull { it.startDatetime }

        val status = when {
            chosen == null -> "PASSATO"
            isActiveNow(chosen, now) -> "IN_CORSO"
            chosen.startDatetime.isAfter(now) || chosen.startDatetime.isEqual(now) -> "PROSSIMO"
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

    private fun chooseRelevantOccurrence(
        occurrences: List<EventOccurrence>,
        now: LocalDateTime
    ): EventOccurrence? {
        val sorted = occurrences.sortedBy { it.startDatetime }

        val active = sorted.firstOrNull { occurrence ->
            isActiveNow(occurrence, now)
        }

        val upcoming = sorted.firstOrNull { occurrence ->
            occurrence.startDatetime.isAfter(now) ||
                    occurrence.startDatetime.isEqual(now)
        }

        return active ?: upcoming
    }

    private fun toEventListDto(
        occurrence: EventOccurrence,
        now: LocalDateTime
    ): EventListDto {
        val event = occurrence.event

        val status = if (isActiveNow(occurrence, now)) {
            "IN_CORSO"
        } else {
            "PROSSIMO"
        }

        return EventListDto(
            id = event.id,
            title = event.title,
            category = event.category?.name,
            address = event.address,
            nextOccurrence = occurrence.startDatetime,
            status = status
        )
    }

    private fun isActiveNow(
        occurrence: EventOccurrence,
        now: LocalDateTime
    ): Boolean {
        val start = occurrence.startDatetime
        val end = occurrence.endDatetime

        return if (end != null) {
            (start.isBefore(now) || start.isEqual(now)) &&
                    (end.isAfter(now) || end.isEqual(now))
        } else {
            start.toLocalDate().isEqual(now.toLocalDate())
        }
    }

    private fun haversine(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusKm = 6371.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}