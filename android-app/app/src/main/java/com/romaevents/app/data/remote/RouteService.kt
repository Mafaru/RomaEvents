package com.romaevents.app.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import org.osmdroid.util.GeoPoint
//this class is used to get the route from the server
class RouteService {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        expectSuccess = true
    }

    //This function takes the starting and ending coordinates and calls the OpenRouteService API to get the walking route between them. It constructs the request with the necessary headers and body, sends the POST request, and returns the response as a string. If the API key is not configured or if the response is invalid, it throws an exception.
    suspend fun getWalkingRoute(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double
    ): List<GeoPoint> {
        return try {
            val response: JsonObject = client.get("https://roma-events-backend.onrender.com/route/walking") {
                header("Accept", "application/json, application/geo+json, application/geo+json; charset=utf-8")
                parameter("startLat", startLat)
                parameter("startLon", startLon)
                parameter("endLat", endLat)
                parameter("endLon", endLon)
            }.body()

            val features = response["features"]?.jsonArray
                ?: throw RuntimeException("Risposta API invalida: manca 'features'")

            if (features.isEmpty()) {
                throw RuntimeException("Nessun percorso trovato per queste coordinate")
            }

            val geometry = features[0].jsonObject["geometry"]?.jsonObject
                ?: throw RuntimeException("Dati geometria mancanti nella risposta")

            val coordinates = geometry["coordinates"]?.jsonArray
                ?: throw RuntimeException("Coordinate del percorso non trovate")

            coordinates.map { item ->
                val pair = item.jsonArray
                val lon = pair[0].jsonPrimitive.double
                val lat = pair[1].jsonPrimitive.double
                GeoPoint(lat, lon)
            }
        } catch (e: ResponseException) {
            val errorBody = e.response.bodyAsText()
            throw RuntimeException("Errore Server Route (${e.response.status.value}): $errorBody")
        } catch (e: Exception) {
            val message = e.message ?: "Errore di tipo ${e.javaClass.simpleName}"
            throw RuntimeException(message)
        }
    }
}
