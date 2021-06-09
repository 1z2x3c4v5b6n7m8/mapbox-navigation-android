package com.mapbox.navigation.ui.maneuver

import android.util.Log
import com.mapbox.api.directions.v5.models.*
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.internal.utils.isSameRoute
import com.mapbox.navigation.ui.maneuver.model.*
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.RuntimeException

internal class ManeuverProcessorV2 {

    private val maneuverState = ManeuverState()

    /*suspend fun requestShields(
        maneuvers: List<ManeuverV2>
    ): HashMap<ManeuverV2, ByteArray?> {
        val routeShieldMap = hashMapOf<ManeuverV2, ByteArray?>()
        maneuvers.forEach { maneuverV2 ->
            val primaryComponents = maneuverV2.primary.componentList
            primaryComponents.forEach { component ->
                val node = component.node
                if (node is RoadShieldComponentNode) {
                    val shield = downloadShield(node)
                    routeShieldMap[maneuverV2] = shield
                }
            }
            val secondaryComponents = maneuverV2.secondary?.componentList
            secondaryComponents?.forEach { component ->
                val node = component.node
                if (node is RoadShieldComponentNode) {
                    val shield = downloadShield(node)
                    routeShieldMap[maneuverV2] = shield
                }
            }
            val subComponents = maneuverV2.sub?.componentList
            subComponents?.forEach { component ->
                val node = component.node
                if (node is RoadShieldComponentNode) {
                    val shield = downloadShield(node)
                    routeShieldMap[maneuverV2] = shield
                }
            }
        }
        maneuverState.allShields = routeShieldMap
        return maneuverState.allShields
    }*/

    /*private suspend fun downloadShield(node: RoadShieldComponentNode): ByteArray? {
        return ifNonNull(node.shieldUrl) { url ->
            val roadShieldRequest = getHttpRequest(url)
            val routeShield = downloadImage(roadShieldRequest).data
            ifNonNull(routeShield) { shield ->
                shield
            }
        }
    }*/

    /*private fun getHttpRequest(imageBaseUrl: String): HttpRequest {
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
    }*/

    fun process(action: ManeuverActionV2): ManeuverResultV2 {
        return when (action) {
            is ManeuverActionV2.GetManeuverListWithRoute -> {
                //processManeuverList(action.route, action.routeLeg)
                ManeuverResultV2.GetManeuverList.Failure("")
            }
            is ManeuverActionV2.GetManeuverList -> {
                processManeuverList(action.routeProgress)
                //ManeuverResultV2.GetManeuverListWithProgress.Failure("")
            }
        }
    }

    private fun processManeuverList(
        route: DirectionsRoute,
        routeLeg: RouteLeg? = null
    ): ManeuverResultV2.GetManeuverList {
        if (!route.isSameRoute(maneuverState.route)) {
            maneuverState.route = route
            maneuverState.allManeuvers.clear()
            try {
                createManeuverList(route)
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
        routeProgress: RouteProgress
    ): ManeuverResultV2.GetManeuverListWithProgress {
        return try {
            val route = routeProgress.route
            val currentBanner = routeProgress.bannerInstructions
            val routeLeg = routeProgress.currentLegProgress?.routeLeg
            val stepDistanceRemaining = getStepDistanceRemaining(routeProgress)
            val stepIndex = routeProgress.currentLegProgress?.currentStepProgress?.stepIndex
            ifNonNull(currentBanner) { banner ->
                val currentManeuver = transformToManeuver(banner, routeProgress)
                if (!route.isSameRoute(maneuverState.route)) {
                    maneuverState.route = route
                    maneuverState.allManeuvers.clear()
                    createManeuverList(route)
                }
                val filteredList = ifNonNull(stepIndex, routeLeg) { index, leg ->
                    createFilteredList(
                        index,
                        leg,
                        currentManeuver,
                        maneuverState.allManeuvers,
                        stepDistanceRemaining
                    )
                }
                ifNonNull(filteredList) {
                    ManeuverResultV2.GetManeuverListWithProgress.Success(it)
                } ?: ManeuverResultV2.GetManeuverListWithProgress.Failure("")
            } ?: ManeuverResultV2.GetManeuverListWithProgress.Failure("$currentBanner cannot be null")
        } catch (exception: Exception) {
            ManeuverResultV2.GetManeuverListWithProgress.Failure(exception.message)
        }
    }

    private fun createManeuverList(route: DirectionsRoute) {
        ifNonNull(route.legs()) { routeLegs ->
            routeLegs.forEach { routeLeg ->
                ifNonNull(routeLeg?.steps()) { steps ->
                    val stepList = mutableListOf<StepIndexToManeuvers>()
                    for (stepIndex in 0..steps.lastIndex) {
                        steps[stepIndex].bannerInstructions()?.let { bannerInstruction ->
                            val maneuverList = mutableListOf<ManeuverV2>()
                            bannerInstruction.forEach { banner ->
                                maneuverList.add(transformToManeuver(banner))
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
                }?: throw RuntimeException("")
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
        stepDistanceRemaining: StepDistanceRemaining
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
        stepDistanceRemaining: StepDistanceRemaining
    ): Int {
        var maneuverIndex = Int.MIN_VALUE
        if (legStep.maneuverList.size == 1) {
            if (currentManeuver == legStep.maneuverList[0]) {
                legStep.maneuverList[0].distanceRemaining = stepDistanceRemaining
            }
        } else {
            for (i in 0..legStep.maneuverList.lastIndex) {
                if (currentManeuver == legStep.maneuverList[i]) {
                    maneuverIndex = i
                    legStep.maneuverList[i].distanceRemaining = stepDistanceRemaining
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
                if (maneuverList.size > 1 && maneuverIndex != Int.MIN_VALUE) {
                    list.addAll(maneuverList.subList(maneuverIndex, maneuverList.size))
                } else if (maneuverList.isNotEmpty() && maneuverList.size == 1) {
                    list.addAll(maneuverList)
                }
            }
        }
        return list
    }

    private fun transformToManeuver(
        bannerInstructions: BannerInstructions,
        routeProgress: RouteProgress? = null
    ): ManeuverV2 {
        val primaryManeuver = getPrimaryManeuver(bannerInstructions.primary())
        val secondaryManeuver = getSecondaryManeuver(bannerInstructions.secondary())
        val subManeuver = getSubManeuverText(bannerInstructions.sub())
        val totalStepDistance = TotalStepDistance(bannerInstructions.distanceAlongGeometry())
        val stepDistanceRemaining = ifNonNull(routeProgress) {
            getStepDistanceRemaining(it)
        } ?: StepDistanceRemaining(bannerInstructions.distanceAlongGeometry())
        return ManeuverV2(
            primaryManeuver,
            totalStepDistance,
            stepDistanceRemaining,
            secondaryManeuver,
            subManeuver,
            null
        )
    }

    private fun getStepDistanceRemaining(
        routeProgress: RouteProgress?
    ): StepDistanceRemaining {
        return ifNonNull(routeProgress?.currentLegProgress?.currentStepProgress) {
            StepDistanceRemaining(it.distanceRemaining.toDouble())
        } ?: StepDistanceRemaining(null)
    }

    private fun getPrimaryManeuver(bannerText: BannerText): PrimaryManeuver {
        val bannerComponentList = bannerText.components()
        return when (!bannerComponentList.isNullOrEmpty()) {
            true -> {
                PrimaryManeuver
                    .Builder()
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
                        .shieldIcon(null)
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
