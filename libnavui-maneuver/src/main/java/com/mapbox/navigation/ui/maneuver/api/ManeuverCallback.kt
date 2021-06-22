package com.mapbox.navigation.ui.maneuver.api

import com.mapbox.bindgen.Expected
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError

interface ManeuverCallback {

    fun onError(error: Expected<ManeuverError, Maneuver>)

    fun onManeuvers(maneuvers: Expected<ManeuverError, List<Maneuver>>)
}
