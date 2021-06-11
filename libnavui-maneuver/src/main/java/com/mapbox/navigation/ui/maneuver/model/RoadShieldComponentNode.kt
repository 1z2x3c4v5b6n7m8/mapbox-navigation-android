package com.mapbox.navigation.ui.maneuver.model

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerText

/**
 * [ComponentNode] of the type [BannerComponents.ICON]
 * @property text String holds [BannerComponents.text] contained inside [BannerComponents]
 * of type [BannerComponents.ICON]
 * @property roadShield RoadShield contains roadshield information.
 *
 * E.g.
 * For the given [BannerText]
 * "primary": {
 *      "components": [
 *          {
 *              "type": "exit",
 *              "text": "Exit"
 *          },
 *          {
 *              "type": "exit-number",
 *              "text": "23"
 *          }
 *          {
 *              "imageBaseURL": "https://mapbox-navigation-shields...",
 *              "type": "icon",
 *              "text": "I-880"
 *          },
 *          {
 *              "type": "delimiter",
 *              "text": "/"
 *          },
 *          {
 *              "type": "text",
 *              "text": "Nimitz Freeway"
 *          }
 *          ...
 *      ],
 *      "type": "turn",
 *      "modifier": "right",
 *      "text": "Exit 23 I-880 / Nimitz Freeway"
 * }
 */

class RoadShieldComponentNode private constructor(
    val text: String,
    val shieldUrl: String? = null,
    val shieldIcon: ByteArray? = null,
    val roadShield: RoadShield
) : ComponentNode {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoadShieldComponentNode

        if (text != other.text) return false
        if (roadShield != other.roadShield) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + (roadShield.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RoadShieldComponentNode(text='$text', roadShield=$roadShield)"
    }


    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder {
        return Builder()
            .text(text)
            .roadShield(roadShield)
    }

    /**
     * Build a new [RoadShieldComponentNode]
     * @property text String
     * @property shieldIcon ByteArray?
     */
    class Builder {
        private var text: String = ""
        private var shieldUrl: String? = null
        private var shieldIcon: ByteArray? = null
        private var roadShield: RoadShield = RoadShield()

        /**
         * apply text to the Builder.
         * @param text String
         * @return Builder
         */
        fun text(text: String): Builder =
            apply { this.text = text }

        /**
         * apply text to the Builder.
         * @param text String
         * @return Builder
         */
        fun shieldUrl(shieldUrl: String?): Builder =
            apply { this.shieldUrl = shieldUrl }

        /**
         * apply text to the Builder.
         * @param text String
         * @return Builder
         */
        fun shieldIcon(shieldIcon: ByteArray?): Builder =
            apply { this.shieldIcon = shieldIcon }

        /**
         * apply roadShield to the Builder.
         * @param roadShield RoadShield?
         * @return Builder
         */
        fun roadShield(roadShield: RoadShield): Builder =
            apply { this.roadShield = roadShield }

        /**
         * Build the [RoadShieldComponentNode]
         * @return RoadShieldComponentNode
         */
        fun build(): RoadShieldComponentNode {
            return RoadShieldComponentNode(
                text,
                null,
                null,
                roadShield,
            )
        }
    }
}
