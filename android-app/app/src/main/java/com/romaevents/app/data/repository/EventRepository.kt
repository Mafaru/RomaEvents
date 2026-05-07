package com.romaevents.app.data.repository

import com.romaevents.app.model.Event
import com.romaevents.app.model.EventDetail
import com.romaevents.app.model.EventMapItem
import com.romaevents.app.model.WeatherResponse
import com.romaevents.app.data.api.ApiService

class EventRepository {

    suspend fun getEvents(): List<Event> {
        return ApiService.getEvents()
    }

    suspend fun getEventDetail(id: Long): EventDetail {
        return ApiService.getEventDetail(id)
    }

    suspend fun searchEvents(query: String): List<Event> {
        return ApiService.searchEvents(query)
    }

    suspend fun getMapEvents(
        lat: Double,
        lon: Double,
        radiusKm: Double = 10.0
    ): List<EventMapItem> {
        return ApiService.getMapEvents(lat, lon, radiusKm)
    }

    suspend fun getWeather(lat: Double, lon: Double): WeatherResponse {
        return ApiService.getWeather(lat, lon)
    }
}