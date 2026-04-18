package com.GoodRoad.model.gh

import com.fasterxml.jackson.annotation.JsonProperty

data class GraphHopperResponse(
    val path: List<Path>?,
    val info: Info?
)

data class Path(
    val distance: Double,
    val time: Long,
    val points: String,
    @JsonProperty("points_encoded")
    val pointsEncoded: Boolean = false
)

data class Info(
    val took: Double
)