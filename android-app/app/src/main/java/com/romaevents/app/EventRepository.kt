package com.romaevents.app

class EventRepository {

    suspend fun getEvents(): List<Event> {
        return ApiService.getEvents()
    }

    suspend fun getEventDetail(id: Long): EventDetail {
        return ApiService.getEventDetail(id)
    }

    suspend fun getMapEvents(
        lat: Double,
        lon: Double,
        radiusKm: Double = 10.0
    ): List<EventMapItem> {
        return ApiService.getMapEvents(lat, lon, radiusKm)
    }
}