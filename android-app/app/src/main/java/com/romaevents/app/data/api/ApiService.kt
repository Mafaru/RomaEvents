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
import kotlinx.serialization.json.*

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
    }

    //this function is used to get the list of events from the server, it uses the handleRequest function to handle the response and errors, it returns a list of Event objects
    suspend fun getEvents(): List<Event> {
        return handleRequest { client.get("$BASE_URL/events").body() }
    }

    //this function is used to search for events based on a query string, it sends a GET request to the server with the query parameter and returns a list of Event objects that match the search criteria
    suspend fun searchEvents(query: String): List<Event> {
        return handleRequest {
            client.get("$BASE_URL/events/search") {
                parameter("query", query)
            }.body()
        }
    }

    //this function is used to get the details of a specific event by its ID, it sends a GET request to the server with the event ID in the URL and returns an EventDetail object containing all the information about the event
    suspend fun getEventDetail(id: Long): EventDetail {
        return handleRequest { client.get("$BASE_URL/events/$id").body() }
    }

    //this function is used to get the list of events to be displayed on the map, it takes the user's latitude and longitude and an optional radius in kilometers, it sends a GET request to the server with these parameters and returns a list of EventMapItem objects that are within the specified radius from the user's location
    suspend fun getMapEvents(
        lat: Double,
        lon: Double,
        radiusKm: Double = 10.0
    ): List<EventMapItem> {
        return handleRequest {
            client.get("$BASE_URL/events/map") {
                parameter("lat", lat)
                parameter("lon", lon)
                parameter("radiusKm", radiusKm)
            }.body()
        }
    }

    //this function is used to get the weather information for a specific location based on its latitude and longitude, it sends a GET request to the server with these parameters and returns a WeatherResponse object containing the temperature, weather description, humidity, and wind speed for that location
    suspend fun getWeather(lat: Double, lon: Double): WeatherResponse {
        return handleRequest {
            client.get("$BASE_URL/weather") {
                parameter("lat", lat)
                parameter("lon", lon)
            }.body()
        }
    }

    //this function is used to authenticate a user by sending their email and password to the server, it sends a POST request to the server with the login credentials in the body and returns an AuthResponse object containing the authentication token and user information if the login is successful
    suspend fun login(email: String, password: String): AuthResponse {
        return handleRequest {
            client.post("$BASE_URL/auth/login") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(LoginRequest(email, password))
            }.body()
        }
    }

    //this function is used to register a new user by sending their username, email, and password to the server, it sends a POST request to the server with the registration information in the body and returns an AuthResponse object containing the authentication token and user information if the registration is successful
    suspend fun register(username: String, email: String, password: String): AuthResponse {
        return handleRequest {
            client.post("$BASE_URL/auth/register") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(RegisterRequest(username, email, password))
            }.body()
        }
    }

    //this is a helper function that handles the API requests and responses, it takes a lambda block that performs the actual API call and returns the result, it catches any ResponseException thrown by the Ktor client and extracts the error message from the response body, if available, and throws an ApiException with the status code and message. It also catches any other exceptions and throws a RuntimeException with a generic error message.
    private suspend inline fun <T> handleRequest(block: () -> T): T {
        return try {
            block()
        } catch (e: ResponseException) {
            val body = e.response.bodyAsText()
            val message = extractErrorMessage(body) ?: "Errore ${e.response.status.value}"
            throw ApiException(e.response.status.value, message)
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException(e.message ?: "Errore di connessione")
        }
    }

    //this function is used to extract the error message from the response body of a failed API request, it tries to parse the body as JSON and look for common fields like "message" or "error" to get a user-friendly error message, if the body is not valid JSON or does not contain these fields, it returns the raw body text if it's short enough, or null otherwise
    private fun extractErrorMessage(body: String): String? {
        if (body.isBlank()) return null
        return try {
            val json = jsonConfig.parseToJsonElement(body).jsonObject
            json["message"]?.jsonPrimitive?.contentOrNull
                ?: json["error"]?.jsonPrimitive?.contentOrNull
        } catch (e: Exception) {
            if (body.length < 100) body else null
        }
    }
}