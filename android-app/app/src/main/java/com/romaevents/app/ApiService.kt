package com.romaevents.app

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import io.ktor.client.request.setBody
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.client.request.accept
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

object ApiService {

    private const val BASE_URL = "http://172.20.10.3:8081"

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
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

    suspend fun getWeather(lat: Double, lon: Double): WeatherResponse {
        return client.get("$BASE_URL/weather") {
            parameter("lat", lat)
            parameter("lon", lon)
        }.body()
    }

    suspend fun login(email: String, password: String): AuthResponse {
        val response = client.post("$BASE_URL/auth/login") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }

        if (!response.status.isSuccess()) {
            throw RuntimeException(response.bodyAsText())
        }

        return response.body()
    }

    suspend fun register(username: String, email: String, password: String): AuthResponse {
        val response = client.post("$BASE_URL/auth/register") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(RegisterRequest(username, email, password))
        }

        if (!response.status.isSuccess()) {
            throw RuntimeException(response.bodyAsText())
        }

        return response.body()
    }


}