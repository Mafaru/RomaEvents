package com.romaevents.backend.event

import com.romaevents.backend.event.dto.EventListDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/events")
class EventController(
    private val eventService: EventService
) {

    @GetMapping
    fun getAllEvents(): List<EventListDto> {
        return eventService.getAllEventsForList()
    }

    @GetMapping("/map")
    fun getEventsForMap(): List<EventListDto> {
        return eventService.getEventsForMap()
    }
}