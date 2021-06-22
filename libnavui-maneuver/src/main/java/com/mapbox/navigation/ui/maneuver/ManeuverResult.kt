package com.mapbox.navigation.ui.maneuver

import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.RoadShield

sealed class ManeuverResult {

    sealed class GetManeuverList : ManeuverResult() {
        data class Failure(val error: String?) : GetManeuverList()
        data class Success(val maneuvers: List<Maneuver>) : GetManeuverList()
    }

    sealed class GetManeuverListWithProgress : ManeuverResult() {
        data class Failure(val error: String?) : GetManeuverListWithProgress()
        data class Success(val maneuvers: List<Maneuver>) : GetManeuverListWithProgress()
    }

    sealed class GetRoadShields : ManeuverResult() {
        data class Failure(val error: String?) : GetRoadShields()
        data class Success(val roadShields: Map<String, RoadShield?>) : GetRoadShields()
    }
}
