package com.mapbox.navigation.ui.maneuver

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.ui.maneuver.model.ManeuverV2

sealed class ManeuverResultV2 {

    sealed class GetManeuverList : ManeuverResultV2() {
        data class Failure(val error: String?) : GetManeuverList()
        data class Success(val maneuvers: List<ManeuverV2>) : GetManeuverList()
    }

    sealed class GetManeuverListWithProgress : ManeuverResultV2() {
        data class Failure(val error: String?) : GetManeuverListWithProgress()
        data class Success(val maneuvers: List<ManeuverV2>) : GetManeuverListWithProgress()
    }
}
