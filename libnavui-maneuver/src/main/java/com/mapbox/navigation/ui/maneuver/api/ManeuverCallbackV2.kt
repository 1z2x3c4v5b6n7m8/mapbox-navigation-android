package com.mapbox.navigation.ui.maneuver.api

import com.mapbox.bindgen.Expected
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.maneuver.model.ManeuverV2

interface ManeuverCallbackV2 {

    fun onError(error: Expected<ManeuverV2, ManeuverError>)

    fun onManeuver(maneuver: Expected<ManeuverV2, ManeuverError>)

    fun onManeuversWithProgress(maneuvers: Expected<List<ManeuverV2>, ManeuverError>)

    fun onRouteShield(routeShieldMap: Expected<HashMap<ManeuverV2, ByteArray?>, ManeuverError>)

    fun onManeuvers(maneuvers: Expected<List<ManeuverV2>, ManeuverError>)
}
