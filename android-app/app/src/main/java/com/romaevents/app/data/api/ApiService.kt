package com.romaevents.app.data.api

import com.romaevents.app.model.AuthResponse
import com.romaevents.app.model.Event
import com.romaevents.app.model.EventDetail
import com.romaevents.app.model.EventMapItem
import com.romaevents.app.model.LoginRequest
import com.romaevents.app.model.RegisterRequest
import com.romaevents.app.model.WeatherResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

object ApiService {

    class ApiException(
        val statusCode: Int,
        override val message: String
    ) : RuntimeException(message)

    private const val BASE_URL = "https://roma-events-backend.onrender.com"

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(Android) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(jsonConfig)
        }

        HttpResponseValidator {
            handleResponseExceptionWithRequest { cause, _ ->
                if (cause is ResponseException) {
                    val response = cause.response
                    val message = errorMessageFromResponse(response)
                    throw ApiException(response.status.value, message)
                }
            }
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
        return client.post("$BASE_URL/auth/login") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }.body()
    }

    suspend fun register(username: String, email: String, password: String): AuthResponse {
        return client.post("$BASE_URL/auth/register") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(RegisterRequest(username, email, password))
        }.body()
    }

    private suspend fun errorMessageFromResponse(response: HttpResponse): String {
        val body = response.bodyAsText()
        return extractErrorMessage(body)
    }

    private fun extractErrorMessage(body: String): String {
        if (body.isBlank()) {
            return "Errore sconosciuto"
        }

        return try {
            val jsonElement = jsonConfig.parseToJsonElement(body)

            val obj = jsonElement.jsonObject

            obj["message"]?.jsonPrimitive?.contentOrNull
                ?: obj["error"]?.jsonPrimitive?.contentOrNull
                ?: body
        } catch (e: Exception) {
            body
        }
    }
}