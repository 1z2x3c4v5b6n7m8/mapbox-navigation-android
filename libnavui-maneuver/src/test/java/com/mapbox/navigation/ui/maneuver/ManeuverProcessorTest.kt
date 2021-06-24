package com.mapbox.navigation.ui.maneuver

import com.mapbox.api.directions.v5.models.*
import com.mapbox.api.directions.v5.models.BannerComponents.DELIMITER
import com.mapbox.api.directions.v5.models.BannerComponents.EXIT
import com.mapbox.api.directions.v5.models.BannerComponents.EXIT_NUMBER
import com.mapbox.api.directions.v5.models.BannerComponents.ICON
import com.mapbox.api.directions.v5.models.BannerComponents.TEXT
import com.mapbox.api.directions.v5.models.BannerComponents.builder
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maneuver.model.Component
import com.mapbox.navigation.ui.maneuver.model.DelimiterComponentNode
import com.mapbox.navigation.ui.maneuver.model.ExitComponentNode
import com.mapbox.navigation.ui.maneuver.model.ExitNumberComponentNode
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
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ManeuverProcessorTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun `when maneuver with direction route having invalid banner instruction`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route_invalid_banner_instruction.json")
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            null,
            maneuverState,
            distanceFormatter
        )
        val expected = ManeuverResult.GetManeuverList.Failure(
            "LegStep should have valid banner instructions"
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with direction route having invalid steps`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route_invalid_steps.json")
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            null,
            maneuverState,
            distanceFormatter
        )
        val expected = ManeuverResult.GetManeuverList.Failure("RouteLeg should have valid steps")

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with direction route having invalid legs`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route_invalid_legs.json")
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            null,
            maneuverState,
            distanceFormatter
        )
        val expected = ManeuverResult.GetManeuverList.Failure("Route should have valid legs")

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with direction route having empty banner instructions`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route_empty_banner_instructions.json")
        )
        val routeLeg = null
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            routeLeg,
            maneuverState,
            distanceFormatter
        )
        val expected = ManeuverResult.GetManeuverList.Failure(
            "Maneuver list not found corresponding to $routeLeg"
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with direction route and route leg passed is different`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json")
        )
        val routeLeg = mockk<RouteLeg> {
            every { distance() } returns null
            every { duration() } returns null
            every { durationTypical() } returns null
            every { summary() } returns null
            every { admins() } returns null
            every { steps() } returns null
            every { incidents() } returns null
            every { annotation() } returns null
            every { closures() } returns null
        }
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            routeLeg,
            maneuverState,
            distanceFormatter
        )
        val expected = ManeuverResult.GetManeuverList.Failure(
            "$routeLeg passed is different"
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with direction route and is valid`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json")
        )
        val routeLeg = null
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            routeLeg,
            maneuverState,
            distanceFormatter
        )
        val expected = 4

        val actual = ManeuverProcessor.process(maneuverAction) as
            ManeuverResult.GetManeuverList.Success

        assertEquals(expected, actual.maneuvers.size)
    }

    @Test
    fun `when maneuver with direction route is fetched then call again`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json")
        )
        val routeLeg = null
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            routeLeg,
            maneuverState,
            distanceFormatter
        )

        ManeuverProcessor.process(maneuverAction)

        val route1 = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json")
        )
        val routeLeg1 = null
        val maneuverAction1 = ManeuverAction.GetManeuverListWithRoute(
            route1,
            routeLeg1,
            maneuverState,
            distanceFormatter
        )

        val actual = ManeuverProcessor.process(maneuverAction1) as
            ManeuverResult.GetManeuverList.Success

        assertEquals(21.2, actual.maneuvers[0].stepDistance.totalDistance, 0.0)
    }

    @Test
    fun `when maneuver with route progress having null banner instruction`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route_invalid_banner_instruction.json")
        )
        val routeProgress = mockk<RouteProgress>(relaxed = true)
        every { routeProgress.route } returns route
        every { routeProgress.bannerInstructions } returns null
        mockLegProgress(routeProgress, 45f, 34.0)
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            distanceFormatter
        )
        val expected = ManeuverResult.GetManeuverListWithProgress.Failure(
            "${routeProgress.bannerInstructions} cannot be null"
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with route progress having invalid banner instruction`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route_invalid_banner_instruction.json")
        )
        val routeProgress = mockk<RouteProgress>(relaxed = true)
        every { routeProgress.route } returns route
        mockLegProgress(routeProgress, 45f, 34.0)
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            distanceFormatter
        )
        val expected = ManeuverResult.GetManeuverListWithProgress.Failure(
            "LegStep should have valid banner instructions"
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with route progress having invalid steps`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route_invalid_steps.json")
        )
        val routeProgress = mockk<RouteProgress>(relaxed = true)
        every { routeProgress.route } returns route
        mockLegProgress(routeProgress, 45f, 34.0)
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            distanceFormatter
        )
        val expected = ManeuverResult.GetManeuverListWithProgress.Failure(
            "RouteLeg should have valid steps"
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with route progress having invalid legs`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route_invalid_legs.json")
        )
        val routeProgress = mockk<RouteProgress>(relaxed = true)
        every { routeProgress.route } returns route
        mockLegProgress(routeProgress, 45f, 34.0)
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            distanceFormatter
        )
        val expected = ManeuverResult.GetManeuverListWithProgress.Failure(
            "Route should have valid legs"
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with route progress having different route leg`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json")
        )
        val routeProgress = mockk<RouteProgress>(relaxed = true)
        every { routeProgress.route } returns route
        val legProgress = mockLegProgress(routeProgress, 45f, 34.0)
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            distanceFormatter
        )
        val expected = ManeuverResult.GetManeuverListWithProgress.Failure(
            "Could not find the ${legProgress.routeLeg}"
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with route progress having different step index`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json")
        )
        val routeLeg = route.legs()!![0]
        val routeProgress = mockk<RouteProgress>(relaxed = true)
        every { routeProgress.route } returns route
        val legProgress = mockLegProgress(routeProgress, 45f, 34.0)
        every { legProgress.currentStepProgress?.stepIndex } returns -1
        every { legProgress.routeLeg } returns routeLeg
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            distanceFormatter
        )
        val expected = ManeuverResult.GetManeuverListWithProgress.Failure(
            "Could not find the -1"
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with route progress valid`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json")
        )
        val routeLeg = route.legs()!![0]
        val routeProgress = mockk<RouteProgress>(relaxed = true)
        every { routeProgress.route } returns route
        every { routeProgress.bannerInstructions } returns getMockBannerInstruction(
            { "Laurel Place" }, { 21.2 }
        )
        val legProgress = mockLegProgress(routeProgress, 15f, 34.0)
        every { legProgress.currentStepProgress?.stepIndex } returns 0
        every { legProgress.routeLeg } returns routeLeg
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            distanceFormatter
        )
        val expected = 4

        val actual = ManeuverProcessor.process(maneuverAction) as
            ManeuverResult.GetManeuverListWithProgress.Success

        assertEquals(expected, actual.maneuvers.size)
        assertEquals(15.0, actual.maneuvers[0].stepDistance.distanceRemaining)
    }

    @Test
    fun `when maneuver with route progress is fetched then call again`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json")
        )
        val routeLeg = route.legs()!![0]
        val routeProgress = mockk<RouteProgress>(relaxed = true)
        every { routeProgress.route } returns route
        every { routeProgress.bannerInstructions } returns getMockBannerInstruction(
            { "Laurel Place" }, { 21.2 }
        )
        val legProgress = mockLegProgress(routeProgress, 15f, 34.0)
        every { legProgress.currentStepProgress?.stepIndex } returns 0
        every { legProgress.routeLeg } returns routeLeg
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            distanceFormatter
        )

        ManeuverProcessor.process(maneuverAction)

        val route1 = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json")
        )
        val routeLeg1 = route1.legs()!![0]
        val routeProgress1 = mockk<RouteProgress>(relaxed = true)
        every { routeProgress1.route } returns route1
        every { routeProgress1.bannerInstructions } returns getMockBannerInstruction(
            { "Laurel Place" }, { 21.2 }
        )
        val legProgress1 = mockLegProgress(routeProgress1, 10f, 34.0)
        every { legProgress1.currentStepProgress?.stepIndex } returns 0
        every { legProgress1.routeLeg } returns routeLeg1
        val maneuverAction1 = ManeuverAction.GetManeuverList(
            routeProgress1,
            maneuverState,
            distanceFormatter
        )

        val actual1 = ManeuverProcessor.process(maneuverAction1) as
            ManeuverResult.GetManeuverListWithProgress.Success

        assertEquals(4, actual1.maneuvers.size)
        assertEquals(10.0, actual1.maneuvers[0].stepDistance.distanceRemaining)
    }

    private fun getMockBannerInstruction(
        textPrimary: () -> String,
        distanceAlongGeometry: () -> Double
    ): BannerInstructions {
        val bannerInstructions = mockk<BannerInstructions>()
        every { bannerInstructions.primary() } returns mockBannerText(
            { textPrimary() },
            { listOf(mockBannerComponent(
                { textPrimary() },
                { TEXT },
                abbreviation = { "Laurel Pl" },
                abbreviationPriority = { 0 }))
            },
            { "turn" },
            { "left" },
            { null },
            { null }
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

    @Suppress("SameParameterValue")
    private fun mockLegProgress(
        routeProgress: RouteProgress,
        distance: Float,
        duration: Double
    ): RouteLegProgress {
        val currentLegProgress = mockk<RouteLegProgress>(relaxed = true)
        every { routeProgress.currentLegProgress } returns currentLegProgress
        every { currentLegProgress.currentStepProgress?.distanceRemaining } returns distance
        every { currentLegProgress.durationRemaining } returns duration
        return currentLegProgress
    }
}
