package com.mapbox.navigation.ui.maneuver.model

class ManeuverV2 internal constructor(
    val primary: PrimaryManeuver,
    val totalDistance: TotalStepDistance,
    var distanceRemaining: StepDistanceRemaining,
    val secondary: SecondaryManeuver?,
    val sub: SubManeuver?,
    val laneGuidance: Lane?,
)
