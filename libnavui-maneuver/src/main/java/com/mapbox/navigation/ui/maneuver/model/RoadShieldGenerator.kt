package com.mapbox.navigation.ui.maneuver.model

import android.content.res.Resources
import android.text.SpannableStringBuilder

internal object RoadShieldGenerator {

    fun styleAndGetRoadShield(
        shieldText: String,
        desiredHeight: Int,
        resources: Resources
    ): SpannableStringBuilder {
        val roadShieldBuilder = SpannableStringBuilder(shieldText)
        // TODO: reimplement shield rendering standalone
        /*if (shieldIcon != null && shieldIcon.isNotEmpty()) {
            val stream = ByteArrayInputStream(shieldIcon)
            val svgBitmap = SvgUtil.renderAsBitmapWithHeight(stream, desiredHeight)
            svgBitmap?.let { b ->
                val drawable = b.drawableWithHeight(desiredHeight, resources)
                roadShieldBuilder.setSpan(
                    ImageSpan(drawable),
                    0,
                    shieldText.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }*/
        return roadShieldBuilder
    }
}
