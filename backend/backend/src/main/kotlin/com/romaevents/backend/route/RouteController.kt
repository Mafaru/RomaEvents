package com.romaevents.backend.route

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RouteController(
    private val routeService: RouteService
) {

    @GetMapping("/route/walking", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getWalkingRoute(
        @RequestParam startLat: Double,
        @RequestParam startLon: Double,
        @RequestParam endLat: Double,
        @RequestParam endLon: Double
    ): String {
        return routeService.getWalkingRoute(startLat, startLon, endLat, endLon)
    }
}