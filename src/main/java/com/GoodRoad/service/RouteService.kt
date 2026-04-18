package com.GoodRoad.service

import com.GoodRoad.service.GraphHopperService
import com.GoodRoad.model.RouteRequest
import com.GoodRoad.model.RouteResponse
import com.GoodRoad.model.gh.Path
import org.springframework.stereotype.Service
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.UUID
import com.GoodRoad.obstacle.ObstacleDBService
import org.springframework.web.servlet.function.ServerResponse.async
import kotlinx.coroutines.awaitAll
import com.GoodRoad.model.PathResponse


import kotlin.math.min
import kotlin.math.max

@Service
class RouteService(
    private val graphHopperService: GraphHopperService,
    private val obstacleDBService: ObstacleDBService
) {

    private fun getObstacleInArea(start: String, end: String): List<ObstacleDBService.ObstacleMapItemResp> {
        val (startLat, startLon) = start.split(",").map{ it.toDouble()}
        val (endLat, endLon) = end.split(",").map{ it.toDouble()}

        val minLat = min(startLat, endLat) - 0.01
        val maxLat = max(startLat, endLat) + 0.01
        val minLon = min(startLon, endLon) - 0.01
        val maxLon = max(startLon, endLon) + 0.01

        return obstacleDBService.listInBox(minLat, maxLat, minLon, maxLon)
    }

    private fun isImpossibleForUser(
        obstacle: ObstacleDBService.ObstacleMapItemResp,
        request: RouteRequest
    ): Boolean {
        return when (obstacle.type) {
            "STAIRS" -> {
                request.avoidStairs || request.maxStairsCount == 0
            }
            "ROAD_SLOPE" -> {
                request.maxSlopeAngle != null && (obstacle.severityEstimate ?: 0) > request.maxSlopeAngle
            }
            "POTHOLES" -> {
                obstacle.severityEstimate == 3.toShort()
            }
            "CURB" -> {
                obstacle.severityEstimate == 3.toShort()
            }
            else -> false
        }
    }

    private fun buildModelWithObstacles(
        obstacles: List<ObstacleDBService.ObstacleMapItemResp>,
        request: RouteRequest
    ): Map<String, Any>? {
        val conditions = mutableListOf<Map<String, Any>>()

        obstacles.forEach { obstacle ->
            when (obstacle.type) {
                "STAIRS" -> conditions.add(mapOf(
                    "if" to "road_class == STEPS",
                    "multiply_by" to "0"
                ))
                "POTHOLES" -> conditions.add(mapOf(
                    "if" to "surface == POTHOLES",
                    "multiply_by" to "0"
                ))
                "ROAD_SLOPE" -> request.maxSlopeAngle?.let { maxSlope ->
                    conditions.add(mapOf(
                        "if" to "max_slope > $maxSlope",
                        "multiply_by" to "0"
                    ))
                }
                "SAND", "GRAVEL" -> conditions.add(mapOf(
                    "if" to "surface == ${obstacle.type}",
                    "multiply_by" to "0"
                ))
                "CURB" -> conditions.add(mapOf(
                    "if" to "barrier == KERB",
                    "multiply_by" to "0"
                ))
            }
        }

        return if (conditions.isNotEmpty()) mapOf("priority" to conditions) else null
    }

    suspend fun buildThreeRoutes(request: RouteRequest): RouteResponse = coroutineScope{
        val obstacles = getObstacleInArea(request.start, request.end)

        val impossibleObstacles = obstacles.filter { obstacle ->
            isImpossibleForUser(obstacle, request)
        }

        val fastModel: Map<String, Any>? = null

        val balancedModel = buildModelWithObstacles(
            impossibleObstacles,
            request
        )

        val safeModel = buildModelWithObstacles(
            obstacles,
            request
        )

        val fastDeferred = async { graphHopperService.getRoute(start = request.start, end = request.end, customModel = fastModel) }
        val balancedDeferred = async { graphHopperService.getRoute(start = request.start, end = request.end, customModel = balancedModel) }
        val safeDeferred = async { graphHopperService.getRoute(start = request.start, end = request.end, customModel = safeModel) }

        val fastPath = fastDeferred.await()?.path?.firstOrNull()
        val balancedPath = balancedDeferred.await()?.path?.firstOrNull()
        val safePath = safeDeferred.await()?.path?.firstOrNull()

        RouteResponse(
            id = UUID.randomUUID().toString(),
            paths = listOfNotNull(
                fastPath?.let { toPathResponse(it, "fast") },
                balancedPath?.let { toPathResponse(it, "balanced") },
                safePath?.let { toPathResponse(it, "safe") }
            )
        )
    }

    private fun toPathResponse(ghPath: Path, routeType: String): PathResponse {
        return PathResponse(
            distance = ghPath.distance,
            time = ghPath.time,
            points = ghPath.points,
            pointsEncoded = true,
            routeType = routeType,
        )
    }

}