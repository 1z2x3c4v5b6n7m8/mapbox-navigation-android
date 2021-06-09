package com.mapbox.navigation.ui.maneuver.api

import com.mapbox.bindgen.Expected
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.maneuver.model.ManeuverV2

interface ManeuverCallbackV2 {

    fun onError(error: Expected<ManeuverError, ManeuverV2>)

    fun onManeuvers(maneuvers: Expected<ManeuverError, List<ManeuverV2>>)

    fun onManeuversWithShields(maneuvers: Expected<ManeuverError, List<ManeuverV2>>)
}
