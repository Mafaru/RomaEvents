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

    private val apiKey = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6Ijk3NDM0ZDc0ODVlMDQ5Y2I5Yzg4YWI0MjgwNmVjNTliIiwiaCI6Im11cm11cjY0In0="

    suspend fun getWalkingRoute(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double
    ): List<GeoPoint> {
        return try {
            val response: JsonObject = client.post(
                "https://api.openrouteservice.org/v2/directions/foot-walking/geojson"
            ) {
                // Header Authorization con la chiave
                header("Authorization", apiKey)
                // Header Accept FONDAMENTALE per risolvere l'errore 406
                header("Accept", "application/json, application/geo+json, application/geo+json; charset=utf-8")
                // User-Agent consigliato per evitare blocchi
                header("User-Agent", "RomaEventsApp/1.0")
                
                contentType(ContentType.Application.Json)

                setBody(buildJsonObject {
                    put("coordinates", buildJsonArray {
                        add(buildJsonArray { add(startLon); add(startLat) })
                        add(buildJsonArray { add(endLon); add(endLat) })
                    })
                })
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
            throw RuntimeException("Errore Server ORS (${e.response.status.value}): $errorBody")
        } catch (e: Exception) {
            val message = e.message ?: "Errore di tipo ${e.javaClass.simpleName}"
            throw RuntimeException(message)
        }
    }
}
