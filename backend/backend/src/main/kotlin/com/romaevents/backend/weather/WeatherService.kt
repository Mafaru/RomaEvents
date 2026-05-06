package com.romaevents.backend.weather

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class WeatherService(
    @Value("\${openweather.api.key}")
    private val apiKey: String
) {

    private val restTemplate = RestTemplate()

    fun getWeather(lat: Double, lon: Double): WeatherResponse {
        val url =
            "https://api.openweathermap.org/data/2.5/weather" +
                    "?lat=$lat" +
                    "&lon=$lon" +
                    "&appid=$apiKey" +
                    "&units=metric" +
                    "&lang=it"

        val response = restTemplate.getForObject(url, Map::class.java)
            ?: throw RuntimeException("Risposta meteo non valida")

        val main = response["main"] as? Map<*, *>
            ?: throw RuntimeException("Dati meteo principali mancanti")

        val weatherList = response["weather"] as? List<*>
            ?: throw RuntimeException("Descrizione meteo mancante")

        val weather = weatherList.firstOrNull() as? Map<*, *>
            ?: throw RuntimeException("Descrizione meteo mancante")

        val wind = response["wind"] as? Map<*, *>

        return WeatherResponse(
            temperature = (main["temp"] as Number).toDouble(),
            description = weather["description"] as? String ?: "Meteo non disponibile",
            humidity = (main["humidity"] as Number).toInt(),
            windSpeed = (wind?.get("speed") as? Number)?.toDouble() ?: 0.0
        )
    }
}