package com.mapbox.navigation.ui.maneuver

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.common.HttpMethod
import com.mapbox.common.HttpRequest
import com.mapbox.common.UAComponents
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.internal.utils.isSameRoute
import com.mapbox.navigation.ui.maneuver.model.Component
import com.mapbox.navigation.ui.maneuver.model.DelimiterComponentNode
import com.mapbox.navigation.ui.maneuver.model.ExitComponentNode
import com.mapbox.navigation.ui.maneuver.model.ExitNumberComponentNode
import com.mapbox.navigation.ui.maneuver.model.LegToManeuvers
import com.mapbox.navigation.ui.maneuver.model.ManeuverV2
import com.mapbox.navigation.ui.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.ui.maneuver.model.RoadShieldComponentNode
import com.mapbox.navigation.ui.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.ui.maneuver.model.StepIndexToManeuvers
import com.mapbox.navigation.ui.maneuver.model.SubManeuver
import com.mapbox.navigation.ui.maneuver.model.TextComponentNode
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import java.util.UUID

internal class ManeuverProcessorV2 {

    private val maneuverState = ManeuverState()
    private val urlToShieldMap = hashMapOf<String, ByteArray?>()

    private val roadShieldContentManager = RoadShieldContentManager()

    fun process(action: ManeuverActionV2): ManeuverResultV2 {
        return when (action) {
            is ManeuverActionV2.GetManeuverListWithRoute -> {
                processManeuverList(action.route, action.distanceFormatter, action.routeLeg)
            }
            is ManeuverActionV2.GetManeuverList -> {
                processManeuverList(action.routeProgress, action.distanceFormatter)
            }
            else -> {
                throw IllegalArgumentException()
            }
        }
    }

    suspend fun processRoadShields(
        startIndex: Int,
        endIndex: Int,
        maneuvers: List<ManeuverV2>
    ): ManeuverResultV2.GetRoadShields {
        val range = startIndex..endIndex
        val shields = roadShieldContentManager.getShields(
            maneuvers.filterIndexed { index, _ -> index in range }
        )

        return ManeuverResultV2.GetRoadShields.Success(shields)
    }

    private fun getHttpRequest(imageBaseUrl: String): HttpRequest {
        return HttpRequest.Builder()
            .url(imageBaseUrl.plus(SVG))
            .body(byteArrayOf())
            .headers(hashMapOf(Pair(USER_AGENT_KEY, USER_AGENT_VALUE)))
            .method(HttpMethod.GET)
            .uaComponents(
                UAComponents.Builder()
                    .sdkIdentifierComponent(SDK_IDENTIFIER)
                    .build()
            )
            .build()
    }

    private fun processManeuverList(
        route: DirectionsRoute,
        distanceFormatter: DistanceFormatter,
        routeLeg: RouteLeg? = null
    ): ManeuverResultV2.GetManeuverList {
        if (!route.isSameRoute(maneuverState.route)) {
            maneuverState.route = route
            maneuverState.allManeuvers.clear()
            maneuverState.roadShields.clear()
            urlToShieldMap.clear()
            try {
                createManeuverList(route, distanceFormatter)
            } catch (exception: RuntimeException) {
                return ManeuverResultV2.GetManeuverList.Failure(exception.message)
            }
        }
        return try {
            val allManeuverList = readManeuverListWith(maneuverState.allManeuvers, routeLeg)
            ManeuverResultV2.GetManeuverList.Success(allManeuverList)
        } catch (exception: RuntimeException) {
            ManeuverResultV2.GetManeuverList.Failure(exception.message)
        }
    }

    private fun processManeuverList(
        routeProgress: RouteProgress,
        distanceFormatter: DistanceFormatter
    ): ManeuverResultV2.GetManeuverListWithProgress {
        return try {
            val route = routeProgress.route
            val currentBanner = routeProgress.bannerInstructions
            val routeLeg = routeProgress.currentLegProgress?.routeLeg
            val stepDistanceRemaining = getStepDistanceRemaining(routeProgress)
            val stepIndex = routeProgress.currentLegProgress?.currentStepProgress?.stepIndex
            ifNonNull(currentBanner) { banner ->
                val currentManeuver = transformToManeuver(banner, distanceFormatter, routeProgress)
                if (!route.isSameRoute(maneuverState.route)) {
                    maneuverState.route = route
                    maneuverState.allManeuvers.clear()
                    maneuverState.roadShields.clear()
                    urlToShieldMap.clear()
                    createManeuverList(route, distanceFormatter)
                }
                val filteredList = ifNonNull(
                    stepIndex,
                    routeLeg,
                    stepDistanceRemaining
                ) { index, leg, distanceRemaining ->
                    createFilteredList(
                        index,
                        leg,
                        currentManeuver,
                        maneuverState.allManeuvers,
                        distanceRemaining
                    )
                }
                ifNonNull(filteredList) {
                    ManeuverResultV2.GetManeuverListWithProgress.Success(it)
                } ?: ManeuverResultV2.GetManeuverListWithProgress.Failure("")
            }
                ?: ManeuverResultV2.GetManeuverListWithProgress.Failure("$currentBanner cannot be null")
        } catch (exception: Exception) {
            ManeuverResultV2.GetManeuverListWithProgress.Failure(exception.message)
        }
    }

    private fun createManeuverList(route: DirectionsRoute, distanceFormatter: DistanceFormatter) {
        ifNonNull(route.legs()) { routeLegs ->
            routeLegs.forEach { routeLeg ->
                ifNonNull(routeLeg?.steps()) { steps ->
                    val stepList = mutableListOf<StepIndexToManeuvers>()
                    for (stepIndex in 0..steps.lastIndex) {
                        steps[stepIndex].bannerInstructions()?.let { bannerInstruction ->
                            val maneuverList = mutableListOf<ManeuverV2>()
                            bannerInstruction.forEach { banner ->
                                maneuverList.add(transformToManeuver(banner, distanceFormatter))
                            }
                            val stepIndexToManeuvers = StepIndexToManeuvers(
                                stepIndex,
                                maneuverList
                            )
                            stepList.add(stepIndexToManeuvers)
                        } ?: throw RuntimeException("LegStep should have valid banner instructions")
                    }
                    maneuverState.allManeuvers.add(LegToManeuvers(routeLeg, stepList))
                } ?: throw RuntimeException("RouteLeg should have valid steps")
            }
        } ?: throw RuntimeException("Route should have valid legs")
        if (maneuverState.allManeuvers.isEmpty()) {
            throw RuntimeException("Maneuver list could not be created")
        }
    }

    private fun readManeuverListWith(
        list: List<LegToManeuvers>,
        routeLeg: RouteLeg? = null
    ): List<ManeuverV2> {
        if (list.isEmpty()) {
            throw RuntimeException("$list cannot be empty")
        }
        val maneuverList = mutableListOf<ManeuverV2>()
        when (routeLeg == null) {
            true -> {
                list[0].stepIndexToManeuvers.forEach { stepIndexToManeuver ->
                    maneuverList.addAll(stepIndexToManeuver.maneuverList)
                }
            }
            else -> {
                list.find { item -> item.routeLeg == routeLeg }?.let { legToManeuver ->
                    legToManeuver.stepIndexToManeuvers.forEach { stepIndexToManeuver ->
                        maneuverList.addAll(stepIndexToManeuver.maneuverList)
                    }
                } ?: throw RuntimeException("")
            }
        }
        if (maneuverList.isEmpty()) {
            throw RuntimeException("Maneuver list not found corresponding to $routeLeg")
        }
        return maneuverList
    }

    private fun createFilteredList(
        stepIndex: Int,
        routeLeg: RouteLeg,
        currentManeuver: ManeuverV2,
        inputList: List<LegToManeuvers>,
        stepDistanceRemaining: Double
    ): List<ManeuverV2> {
        val legToManeuver = routeLeg.findIn(inputList)
        val stepToManeuverList = legToManeuver.stepIndexToManeuvers
        val legStep = stepIndex.findIn(stepToManeuverList)
        val indexOfLegStep = stepToManeuverList.indexOf(legStep)
        val maneuverIndex = updateDistanceRemaining(legStep, currentManeuver, stepDistanceRemaining)
        return filterList(maneuverIndex, indexOfLegStep, stepToManeuverList)
    }

    private fun RouteLeg.findIn(legs: List<LegToManeuvers>): LegToManeuvers {
        return legs.find {
            it.routeLeg == this
        } ?: throw RuntimeException("Could not find the $this")
    }

    private fun Int.findIn(steps: List<StepIndexToManeuvers>): StepIndexToManeuvers {
        return steps.find {
            it.stepIndex == this
        } ?: throw RuntimeException("Could not find the $this")
    }

    private fun updateDistanceRemaining(
        legStep: StepIndexToManeuvers,
        currentManeuver: ManeuverV2,
        stepDistanceRemaining: Double
    ): Int {
        var maneuverIndex = Int.MIN_VALUE
        if (legStep.maneuverList.size == 1) {
            if (currentManeuver == legStep.maneuverList[0]) {
                legStep.maneuverList[0].stepDistance.distanceRemaining = stepDistanceRemaining
            }
        } else {
            for (i in 0..legStep.maneuverList.lastIndex) {
                if (currentManeuver == legStep.maneuverList[i]) {
                    maneuverIndex = i
                    legStep.maneuverList[i].stepDistance.distanceRemaining = stepDistanceRemaining
                    break
                }
            }
        }
        return maneuverIndex
    }

    private fun filterList(
        maneuverIndex: Int,
        indexOfLegStep: Int,
        inputList: List<StepIndexToManeuvers>
    ): List<ManeuverV2> {
        val list = mutableListOf<ManeuverV2>()
        if (maneuverIndex == Int.MIN_VALUE) {
            for (i in indexOfLegStep..inputList.lastIndex) {
                list.addAll(inputList[i].maneuverList)
            }
        } else {
            for (i in indexOfLegStep..inputList.lastIndex) {
                val maneuverList = inputList[i].maneuverList
                if (i == indexOfLegStep && maneuverList.size > 1) {
                    list.addAll(maneuverList.subList(maneuverIndex, maneuverList.size))
                } else {
                    list.addAll(maneuverList)
                }
            }
        }
        return list
    }

    private fun transformToManeuver(
        bannerInstructions: BannerInstructions,
        distanceFormatter: DistanceFormatter,
        routeProgress: RouteProgress? = null
    ): ManeuverV2 {
        val primaryManeuver = getPrimaryManeuver(bannerInstructions.primary())
        val secondaryManeuver = getSecondaryManeuver(bannerInstructions.secondary())
        val subManeuver = getSubManeuverText(bannerInstructions.sub())
        val totalStepDistance = bannerInstructions.distanceAlongGeometry()
        val stepDistanceRemaining = getStepDistanceRemaining(routeProgress)
        val stepDistance = StepDistance(distanceFormatter, totalStepDistance, stepDistanceRemaining)
        return ManeuverV2(
            primaryManeuver,
            stepDistance,
            secondaryManeuver,
            subManeuver,
            null
        )
    }

    private fun getStepDistanceRemaining(routeProgress: RouteProgress?): Double? {
        return ifNonNull(routeProgress?.currentLegProgress?.currentStepProgress) {
            it.distanceRemaining.toDouble()
        }
    }

    private fun getPrimaryManeuver(bannerText: BannerText): PrimaryManeuver {
        val bannerComponentList = bannerText.components()
        return when (!bannerComponentList.isNullOrEmpty()) {
            true -> {
                PrimaryManeuver
                    .Builder()
                    .id(UUID.randomUUID().toString())
                    .text(bannerText.text())
                    .type(bannerText.type())
                    .degrees(bannerText.degrees())
                    .modifier(bannerText.modifier())
                    .drivingSide(bannerText.drivingSide())
                    .componentList(createComponents(bannerComponentList))
                    .build()
            }
            else -> {
                PrimaryManeuver.Builder().build()
            }
        }
    }

    private fun getSecondaryManeuver(bannerText: BannerText?): SecondaryManeuver? {
        val bannerComponentList = bannerText?.components()
        return when (!bannerComponentList.isNullOrEmpty()) {
            true -> {
                SecondaryManeuver
                    .Builder()
                    .id(UUID.randomUUID().toString())
                    .text(bannerText.text())
                    .type(bannerText.type())
                    .degrees(bannerText.degrees())
                    .modifier(bannerText.modifier())
                    .drivingSide(bannerText.drivingSide())
                    .componentList(createComponents(bannerComponentList))
                    .build()
            }
            else -> {
                null
            }
        }
    }

    private fun getSubManeuverText(bannerText: BannerText?): SubManeuver? {
        bannerText?.let { subBanner ->
            if (subBanner.type() != null && subBanner.text().isNotEmpty()) {
                val bannerComponentList = subBanner.components()
                return when (!bannerComponentList.isNullOrEmpty()) {
                    true -> {
                        SubManeuver
                            .Builder()
                            .id(UUID.randomUUID().toString())
                            .text(bannerText.text())
                            .type(bannerText.type())
                            .degrees(bannerText.degrees())
                            .modifier(bannerText.modifier())
                            .drivingSide(bannerText.drivingSide())
                            .componentList(createComponents(bannerComponentList))
                            .build()
                    }
                    else -> {
                        null
                    }
                }
            }
        }
        return null
    }

    private fun createComponents(
        bannerComponentList: List<BannerComponents>
    ): List<Component> {
        val componentList = mutableListOf<Component>()
        bannerComponentList.forEach { component ->
            when {
                component.type() == BannerComponents.EXIT -> {
                    val exit = ExitComponentNode
                        .Builder()
                        .text(component.text())
                        .build()
                    componentList.add(Component(BannerComponents.EXIT, exit))
                }
                component.type() == BannerComponents.EXIT_NUMBER -> {
                    val exitNumber = ExitNumberComponentNode
                        .Builder()
                        .text(component.text())
                        .build()
                    componentList.add(Component(BannerComponents.EXIT_NUMBER, exitNumber))
                }
                component.type() == BannerComponents.TEXT -> {
                    val text = TextComponentNode
                        .Builder()
                        .text(component.text())
                        .abbr(component.abbreviation())
                        .abbrPriority(component.abbreviationPriority())
                        .build()
                    componentList.add(Component(BannerComponents.TEXT, text))
                }
                component.type() == BannerComponents.DELIMITER -> {
                    val delimiter = DelimiterComponentNode
                        .Builder()
                        .text(component.text())
                        .build()
                    componentList.add(Component(BannerComponents.DELIMITER, delimiter))
                }
                component.type() == BannerComponents.ICON -> {
                    val roadShield = RoadShieldComponentNode
                        .Builder()
                        .text(component.text())
                        .shieldUrl(component.imageBaseUrl())
                        .build()
                    componentList.add(Component(BannerComponents.ICON, roadShield))
                }
            }
        }
        return componentList
    }

    private companion object {
        private const val SVG = ".svg"
        private const val USER_AGENT_KEY = "User-Agent"
        private const val USER_AGENT_VALUE = "MapboxJava/"
        private const val SDK_IDENTIFIER = "mapbox-navigation-ui-android"
    }
}
