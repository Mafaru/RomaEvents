package com.romaevents.backend.event

import com.romaevents.backend.event.dto.EventDetailDto
import com.romaevents.backend.event.dto.EventListDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/events")
class EventController(
    private val eventService: EventService
) {

    @GetMapping
    fun getAllEvents(
        @RequestParam(required = false) categoryId: Long?
    ): List<EventListDto> {
        return if (categoryId != null) {
            eventService.getEventsForListByCategory(categoryId)
        } else {
            eventService.getAllEventsForList()
        }
    }

    @GetMapping("/map")
    fun getEventsForMap(): List<EventListDto> {
        return eventService.getEventsForMap()
    }

    @GetMapping("/{id}")
    fun getEventById(@PathVariable id: Long): EventDetailDto {
        return eventService.getEventById(id)
    }
}