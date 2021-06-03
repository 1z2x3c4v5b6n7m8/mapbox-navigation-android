package com.mapbox.navigation.ui.maneuver

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.trip.model.RouteProgress

sealed class ManeuverActionV2 {

    data class GetManeuverList(
        val routeProgress: RouteProgress,
        val distanceFormatter: DistanceFormatter
    ) : ManeuverActionV2()

    data class GetManeuverListWithRoute(
        val route: DirectionsRoute,
        val routeLeg: RouteLeg? = null,
        val distanceFormatter: DistanceFormatter
    ) : ManeuverActionV2()
}
