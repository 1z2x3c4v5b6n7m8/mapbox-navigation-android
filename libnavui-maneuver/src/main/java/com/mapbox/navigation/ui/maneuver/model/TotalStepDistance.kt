package com.mapbox.navigation.ui.maneuver.model

import com.mapbox.navigation.ui.utils.internal.ifNonNull
import kotlin.math.abs

data class TotalStepDistance(
    val distance: Double
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TotalStepDistance

        if (distance.notEqualDelta(other.distance)) return false

        return true
    }

    override fun hashCode(): Int {
        return distance.hashCode()
    }

    private fun Double?.notEqualDelta(other: Double?): Boolean {
        return ifNonNull(this, other) { d1, d2 ->
            abs(d1/d2 - 1) > 0.1
        } ?: false
    }
}
