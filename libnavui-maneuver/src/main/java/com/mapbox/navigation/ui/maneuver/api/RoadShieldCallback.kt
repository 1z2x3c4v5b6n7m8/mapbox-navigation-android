package com.mapbox.navigation.ui.maneuver.api

import com.mapbox.bindgen.Expected
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.maneuver.model.RoadShield

fun interface RoadShieldCallback {

    fun onRoadShields(maneuvers: Expected<ManeuverError, Map<String, RoadShield?>>)
}
