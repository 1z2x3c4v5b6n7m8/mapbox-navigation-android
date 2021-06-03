package com.mapbox.navigation.ui.maneuver.api

import android.graphics.Bitmap
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.maneuver.model.ManeuverV2

interface ManeuverCallbackV2 {

    fun onError(error: Expected<ManeuverV2, ManeuverError>)

    fun onManeuver(maneuver: Expected<ManeuverV2, ManeuverError>)

    fun onManeuversWithProgress(maneuvers: Expected<List<ManeuverV2>, ManeuverError>)

    fun onRouteShield(routeShieldMap: Expected<HashMap<ManeuverV2, ByteArray?>, ManeuverError>)

    fun onManeuvers(maneuvers: Expected<List<ManeuverV2>, ManeuverError>)
}
