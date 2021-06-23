package com.mapbox.navigation.ui.maneuver

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerComponents.DELIMITER
import com.mapbox.api.directions.v5.models.BannerComponents.EXIT
import com.mapbox.api.directions.v5.models.BannerComponents.EXIT_NUMBER
import com.mapbox.api.directions.v5.models.BannerComponents.ICON
import com.mapbox.api.directions.v5.models.BannerComponents.TEXT
import com.mapbox.api.directions.v5.models.BannerComponents.builder
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maneuver.model.Component
import com.mapbox.navigation.ui.maneuver.model.DelimiterComponentNode
import com.mapbox.navigation.ui.maneuver.model.ExitComponentNode
import com.mapbox.navigation.ui.maneuver.model.ExitNumberComponentNode
import com.mapbox.navigation.ui.maneuver.model.Lane
import com.mapbox.navigation.ui.maneuver.model.LaneIndicator
import com.mapbox.navigation.ui.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.ui.maneuver.model.RoadShieldComponentNode
import com.mapbox.navigation.ui.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.ui.maneuver.model.SubManeuver
import com.mapbox.navigation.ui.maneuver.model.TextComponentNode
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ManeuverProcessorTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private fun getMockBannerInstruction(
        textPrimary: () -> String,
        distanceAlongGeometry: () -> Double
    ): BannerInstructions {
        val bannerInstructions = mockk<BannerInstructions>()
        every { bannerInstructions.primary() } returns mockBannerText(
            { textPrimary() },
            { listOf(mockBannerComponent({ textPrimary() }, { TEXT })) }
        )
        every { bannerInstructions.secondary() } returns null
        every { bannerInstructions.sub() } returns null
        every { bannerInstructions.distanceAlongGeometry() } returns distanceAlongGeometry()
        return bannerInstructions
    }

    private fun mockBannerText(
        text: () -> String,
        componentList: () -> List<BannerComponents>,
        type: () -> String = { TEXT },
        modifier: () -> String = { ManeuverModifier.RIGHT },
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

    private fun getPrimaryBannerText(): BannerText {
        val primaryBannerComponentList = getPrimaryBannerComponentList()
        val mockPrimaryBannerText = mockk<BannerText>()
        every { mockPrimaryBannerText.type() } returns "merge"
        every { mockPrimaryBannerText.text() } returns "Exit 23 I-880 / Stevenson Boulevard"
        every { mockPrimaryBannerText.degrees() } returns null
        every { mockPrimaryBannerText.drivingSide() } returns null
        every { mockPrimaryBannerText.modifier() } returns "slight left"
        every { mockPrimaryBannerText.components() } returns primaryBannerComponentList
        return mockPrimaryBannerText
    }

    private fun getPrimaryBannerComponentList(): List<BannerComponents> {
        val primaryExitComponent = buildExitComponent("Exit")
        val primaryExitNumberComponent = buildExitNumberComponent("23")
        val primaryRoadShieldComponent = buildRoadShieldComponent("I-880")
        val primaryDelimitedComponent = buildDelimiterComponent("/")
        val primaryTextComponent = buildTextComponent("Stevenson Boulevard")
        return listOf(
            primaryExitComponent,
            primaryExitNumberComponent,
            primaryRoadShieldComponent,
            primaryDelimitedComponent,
            primaryTextComponent
        )
    }

    private fun createPrimaryManeuver(bannerText: BannerText): PrimaryManeuver {
        val componentList = listOf(
            Component(
                EXIT,
                ExitComponentNode
                    .Builder()
                    .text("Exit")
                    .build()
            ),
            Component(
                EXIT_NUMBER,
                ExitNumberComponentNode
                    .Builder()
                    .text("23")
                    .build()
            ),
            Component(
                ICON,
                RoadShieldComponentNode
                    .Builder()
                    .text("I-880")
                    .build()
            ),
            Component(
                DELIMITER,
                DelimiterComponentNode
                    .Builder()
                    .text("/")
                    .build()
            ),
            Component(
                TEXT,
                TextComponentNode
                    .Builder()
                    .text("Stevenson Boulevard")
                    .abbr(null)
                    .abbrPriority(null)
                    .build()
            )
        )
        return PrimaryManeuver
            .Builder()
            .text(bannerText.text())
            .type(bannerText.type())
            .degrees(bannerText.degrees())
            .modifier(bannerText.modifier())
            .drivingSide(bannerText.drivingSide())
            .componentList(componentList)
            .build()
    }

    private fun getSecondaryBannerText(): BannerText {
        val bannerComponentList = getSecondaryBannerComponentList()
        val mockBannerText = mockk<BannerText>()
        every { mockBannerText.type() } returns "fork"
        every { mockBannerText.text() } returns "Mowry Avenue"
        every { mockBannerText.degrees() } returns null
        every { mockBannerText.drivingSide() } returns null
        every { mockBannerText.modifier() } returns "left"
        every { mockBannerText.components() } returns bannerComponentList
        return mockBannerText
    }

    private fun getSecondaryBannerComponentList(): List<BannerComponents> {
        val secondaryTextComponent = buildTextComponent("Mowry Avenue")
        return listOf(
            secondaryTextComponent
        )
    }

    private fun createSecondaryManeuver(bannerText: BannerText): SecondaryManeuver {
        return SecondaryManeuver
            .Builder()
            .text(bannerText.text())
            .type(bannerText.type())
            .degrees(bannerText.degrees())
            .modifier(bannerText.modifier())
            .drivingSide(bannerText.drivingSide())
            .componentList(
                listOf(
                    Component(
                        TEXT,
                        TextComponentNode
                            .Builder()
                            .text("Mowry Avenue")
                            .abbr(null)
                            .abbrPriority(null)
                            .build()
                    )
                )
            )
            .build()
    }

    private fun getSubBannerText(): BannerText {
        val bannerComponentList = getSubBannerComponentList()
        val mockBannerText = mockk<BannerText>()
        every { mockBannerText.type() } returns "turn"
        every { mockBannerText.text() } returns "Central Fremont"
        every { mockBannerText.degrees() } returns null
        every { mockBannerText.drivingSide() } returns null
        every { mockBannerText.modifier() } returns "right"
        every { mockBannerText.components() } returns bannerComponentList
        return mockBannerText
    }

    private fun getLaneBannerText(): BannerText {
        val bannerComponentList = buildLaneComponent()
        val mockBannerText = mockk<BannerText>()
        every { mockBannerText.type() } returns null
        every { mockBannerText.text() } returns ""
        every { mockBannerText.degrees() } returns null
        every { mockBannerText.drivingSide() } returns null
        every { mockBannerText.modifier() } returns null
        every { mockBannerText.components() } returns bannerComponentList
        return mockBannerText
    }

    private fun getSubBannerComponentList(): List<BannerComponents> {
        val subRoadShieldComponent = buildRoadShieldComponent("I-880")
        val subDelimitedComponent = buildDelimiterComponent("/")
        val subTextComponent = buildTextComponent("Central Fremont")
        return listOf(
            subRoadShieldComponent,
            subDelimitedComponent,
            subTextComponent
        )
    }

    private fun createSubManeuver(bannerText: BannerText): SubManeuver {
        val componentList = listOf(
            Component(
                ICON,
                RoadShieldComponentNode
                    .Builder()
                    .text("I-880")
                    .build()
            ),
            Component(
                DELIMITER,
                DelimiterComponentNode
                    .Builder()
                    .text("/")
                    .build()
            ),
            Component(
                TEXT,
                TextComponentNode
                    .Builder()
                    .text("Central Fremont")
                    .abbr(null)
                    .abbrPriority(null)
                    .build()
            )
        )
        return SubManeuver
            .Builder()
            .text(bannerText.text())
            .type(bannerText.type())
            .degrees(bannerText.degrees())
            .modifier(bannerText.modifier())
            .drivingSide(bannerText.drivingSide())
            .componentList(componentList)
            .build()
    }

    private fun createLaneManeuver(): List<LaneIndicator> {
        return listOf(
            LaneIndicator
                .Builder()
                .isActive(false)
                .directions(listOf("left"))
                .build(),
            LaneIndicator
                .Builder()
                .isActive(false)
                .directions(listOf("right"))
                .build(),
            LaneIndicator
                .Builder()
                .isActive(true)
                .directions(listOf("straight"))
                .build()
        )
    }

    private fun buildExitComponent(text: String): BannerComponents {
        return builder()
            .type(EXIT)
            .text(text)
            .build()
    }

    private fun buildExitNumberComponent(text: String): BannerComponents {
        return builder()
            .type(EXIT_NUMBER)
            .text(text)
            .build()
    }

    private fun buildDelimiterComponent(text: String): BannerComponents {
        return builder()
            .type(DELIMITER)
            .text(text)
            .build()
    }

    private fun buildRoadShieldComponent(text: String): BannerComponents {
        return builder()
            .type(ICON)
            .text(text)
            .build()
    }

    private fun buildTextComponent(text: String): BannerComponents {
        return builder()
            .type(TEXT)
            .text(text)
            .build()
    }

    private fun buildLaneComponent(): List<BannerComponents> {
        return listOf(
            builder()
                .type("lane")
                .text("")
                .active(false)
                .directions(listOf("left"))
                .build(),
            builder()
                .type("lane")
                .text("")
                .active(false)
                .directions(listOf("right"))
                .build(),
            builder()
                .type("lane")
                .text("")
                .active(true)
                .directions(listOf("straight"))
                .build()
        )
    }
}
