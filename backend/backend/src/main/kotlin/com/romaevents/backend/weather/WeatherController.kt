package com.romaevents.backend.weather

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class WeatherController(
    private val weatherService: WeatherService
) {

    @GetMapping("/weather")
    fun getWeather(
        @RequestParam lat: Double,
        @RequestParam lon: Double
    ): WeatherResponse {
        return weatherService.getWeather(lat, lon)
    }
}