package com.GoodRoad.controller

import com.GoodRoad.model.RouteRequest
import com.GoodRoad.model.RouteResponse
import com.GoodRoad.service.RouteService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/v1/routes")
class RouteController (private val routeService: RouteService) {
    @PostMapping
    suspend fun buildRoute (
        @Valid @RequestBody request: RouteRequest
    ): ResponseEntity<RouteResponse> {
        return try {
            val response = routeService.buildThreeRoutes(request)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid request: ${e.message}"
            )
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to build route: ${e.message}"
            )
        }
    }

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "status" to "OK",
                "service" to "route-service"
            )
        )
    }
}
