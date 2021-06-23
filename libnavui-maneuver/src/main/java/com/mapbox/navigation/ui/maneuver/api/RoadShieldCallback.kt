package com.mapbox.navigation.ui.maneuver.api

import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.RoadShield
import com.mapbox.navigation.ui.maneuver.model.RoadShieldError

fun interface RoadShieldCallback {
    fun onRoadShields(
        maneuvers: List<Maneuver>,
        shields: Map<String, RoadShield?>,
        errors: Map<String, RoadShieldError>
    )
}
