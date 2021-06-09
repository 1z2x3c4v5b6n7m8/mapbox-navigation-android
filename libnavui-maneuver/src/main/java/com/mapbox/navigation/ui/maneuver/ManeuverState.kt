package com.mapbox.navigation.ui.maneuver

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.ui.maneuver.model.LegToManeuvers
import java.util.concurrent.CopyOnWriteArrayList

internal data class ManeuverState(
    var route: DirectionsRoute? = null,
    val allManeuvers: MutableList<LegToManeuvers> = CopyOnWriteArrayList()
)
