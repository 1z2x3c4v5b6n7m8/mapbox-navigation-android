package com.mapbox.navigation.ui.maneuver

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.ui.maneuver.model.LegToManeuvers

internal data class ManeuverState(
    var route: DirectionsRoute?,
    val allManeuvers: MutableList<LegToManeuvers>
)
