package com.romaevents.backend.route

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class RouteService(
    @Value("\${openrouteservice.api.key:}")
    private val apiKey: String
) {

    private val restTemplate = RestTemplate()

    fun getWalkingRoute(startLat: Double, startLon: Double, endLat: Double, endLon: Double): String {
        if (apiKey.isBlank()) {
            throw IllegalStateException("OPENROUTESERVICE_API_KEY non configurata")
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            accept = listOf(MediaType.APPLICATION_JSON, MediaType.valueOf("application/geo+json"))
            set("Authorization", apiKey)
            set("User-Agent", "RomaEventsBackend/1.0")
        }

        val body = mapOf(
            "coordinates" to listOf(
                listOf(startLon, startLat),
                listOf(endLon, endLat)
            )
        )

        val response = restTemplate.exchange(
            "https://api.openrouteservice.org/v2/directions/foot-walking/geojson",
            HttpMethod.POST,
            HttpEntity(body, headers),
            String::class.java
        )

        return response.body ?: throw RuntimeException("Risposta route non valida")
    }
}