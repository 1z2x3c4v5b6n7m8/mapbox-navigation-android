package com.mapbox.navigation.ui.maneuver.model

data class RoadShield(
    val shieldUrl: String? = null,
    var shieldIcon: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoadShield

        if (shieldUrl != other.shieldUrl) return false
        /*if (shieldIcon != null) {
            if (other.shieldIcon == null) return false
            if (!shieldIcon.contentEquals(other.shieldIcon)) return false
        } else if (other.shieldIcon != null) return false*/

        return true
    }

    override fun hashCode(): Int {
        var result = shieldUrl?.hashCode() ?: 0
        result = 31 * result + (shieldIcon?.contentHashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "RoadShield(shieldUrl=$shieldUrl, shieldIcon=${shieldIcon?.contentToString()})"
    }
}
