package com.mapbox.navigation.ui.maneuver.model

class ManeuverV2 internal constructor(
    val primary: PrimaryManeuver,
    val totalDistance: TotalStepDistance,
    var distanceRemaining: StepDistanceRemaining,
    val secondary: SecondaryManeuver?,
    val sub: SubManeuver?,
    val laneGuidance: Lane?,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ManeuverV2

        if (primary != other.primary) return false
        if (secondary != other.secondary) return false
        if (sub != other.sub) return false
        if (laneGuidance != other.laneGuidance) return false
        if (totalDistance != other.totalDistance) return false

        return true
    }

    override fun hashCode(): Int {
        var result = primary.hashCode()
        result = 31 * result + (secondary?.hashCode() ?: 0)
        result = 31 * result + (sub?.hashCode() ?: 0)
        result = 31 * result + (laneGuidance?.hashCode() ?: 0)
        return result
    }
}
