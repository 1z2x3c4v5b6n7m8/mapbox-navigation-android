package com.mapbox.navigation.ui.maneuver.api

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.ui.maneuver.model.StepDistance
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MapboxManeuverApiTest {

    private fun mockBannerInstruction(
        textPrimary: () -> String,
        distanceAlongGeometry: () -> Double
    ): BannerInstructions {
        val bannerInstructions = mockk<BannerInstructions>()
        every { bannerInstructions.primary() } returns mockBannerText(
            { textPrimary() },
            { listOf(mockBannerComponent({ textPrimary() }, { BannerComponents.TEXT })) }
        )
        every { bannerInstructions.secondary() } returns null
        every { bannerInstructions.sub() } returns null
        every { bannerInstructions.distanceAlongGeometry() } returns distanceAlongGeometry()
        return bannerInstructions
    }

    private fun mockBannerText(
        text: () -> String,
        componentList: () -> List<BannerComponents>,
        type: () -> String = { MANEUVER_TYPE },
        modifier: () -> String = { MANEUVER_MODIFIER },
        degrees: () -> Double? = { null },
        drivingSide: () -> String? = { null },
    ): BannerText {
        val bannerText = mockk<BannerText>()
        every { bannerText.text() } returns text()
        every { bannerText.type() } returns type()
        every { bannerText.degrees() } returns degrees()
        every { bannerText.modifier() } returns modifier()
        every { bannerText.drivingSide() } returns drivingSide()
        every { bannerText.components() } returns componentList()
        return bannerText
    }

    private fun mockBannerComponent(
        text: () -> String,
        type: () -> String,
        active: () -> Boolean? = { null },
        subType: () -> String? = { null },
        imageUrl: () -> String? = { null },
        directions: () -> List<String>? = { null },
        imageBaseUrl: () -> String? = { null },
        abbreviation: () -> String? = { null },
        abbreviationPriority: () -> Int? = { null },
    ): BannerComponents {
        val bannerComponents = mockk<BannerComponents>()
        every { bannerComponents.text() } returns text()
        every { bannerComponents.type() } returns type()
        every { bannerComponents.active() } returns active()
        every { bannerComponents.subType() } returns subType()
        every { bannerComponents.imageUrl() } returns imageUrl()
        every { bannerComponents.directions() } returns directions()
        every { bannerComponents.imageBaseUrl() } returns imageBaseUrl()
        every { bannerComponents.abbreviation() } returns abbreviation()
        every { bannerComponents.abbreviationPriority() } returns abbreviationPriority()
        return bannerComponents
    }

    private companion object {
        private const val MANEUVER_TYPE = "MANEUVER TYPE"
        private const val MANEUVER_MODIFIER = "MANEUVER MODIFIER"
    }
}
