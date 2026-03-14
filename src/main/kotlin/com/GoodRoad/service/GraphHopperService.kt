package com.GoodRoad.service

import com.GoodRoad.model.gh.GraphHopperResponse
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.beans.factory.annotation.Value

@Component
class GraphHopperService(
    @Value("\${graphhopper.api.key}") private val apiKey: String,
    @Value("\${graphhopper.api.url:https://graphhopper.com/api/1}") private val baseUrl: String
) {
    private val webClient: WebClient = WebClient.builder()
        .baseUrl(baseUrl)
        .codecs {configurer ->configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)}
        .build()

    suspend fun getRoute(
        start: String,
        end: String,
        profile: String = "foot",
        pointsEncoded: Boolean = true,
        locale: String = "ru",
        customModel: Map<String, Any>? = null
    ): GraphHopperResponse? {

        return try {

            val startPart = start.split(",")
            val endPart = end.split(",")

            val requestBody = mapOf(
                "points" to listOf(
                    listOf(startPart[0].toDouble(), startPart[1].toDouble()),
                    listOf(endPart[0].toDouble(), endPart[1].toDouble())
                ),
                "profile" to profile,
                "points_encoded" to pointsEncoded,
                "locale" to "ru",
                "custom_model" to (customModel ?: emptyMap<String, Any>())
            )

            webClient.post().uri("/route").bodyValue(requestBody)
                .header("Content-Type", "application/json")
                .retrieve()
                .awaitBody<GraphHopperResponse>()

        } catch (e: Exception) {
            println("GraphHopper error: ${e.message}")
            null
        }
    }
}