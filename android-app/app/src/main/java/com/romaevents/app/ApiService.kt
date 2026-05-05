package com.romaevents.app

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object ApiService {

    private const val BASE_URL = "http://172.20.10.3:8081"

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun getEvents(): List<Event> {
        return client.get("$BASE_URL/events").body()
    }

    suspend fun searchEvents(query: String): List<Event> {
        return client.get("$BASE_URL/events/search") {
            parameter("query", query)
        }.body()
    }

    suspend fun getEventDetail(id: Long): EventDetail {
        return client.get("$BASE_URL/events/$id").body()
    }

    suspend fun getMapEvents(
        lat: Double,
        lon: Double,
        radiusKm: Double = 10.0
    ): List<EventMapItem> {
        return client.get("$BASE_URL/events/map") {
            parameter("lat", lat)
            parameter("lon", lon)
            parameter("radiusKm", radiusKm)
        }.body()
    }
}