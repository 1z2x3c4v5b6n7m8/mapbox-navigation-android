package com.mapbox.navigation.ui.maneuver

import android.util.Log
import com.mapbox.api.directions.v5.models.*
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState.*
import com.mapbox.navigation.core.internal.utils.isSameRoute
import com.mapbox.navigation.ui.maneuver.model.*
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.RuntimeException

internal class ManeuverProcessorV2 {
    // Every time the API gets a route progress event, it finds if [RouteProgress.bannerInstruction]
    // is contained in [allManeuvers]. If the function finds it, the index at which [RouteProgress.bannerInstruction]
    // exists in [allManeuvers] is represented by this field.
    private var maneuverContainedAtIndex = Int.MIN_VALUE

    private val maneuverState = ManeuverState(null, CopyOnWriteArrayList())

    private val maneuverListWithProgress = mutableListOf<ManeuverV2>()

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
        return getAllManeuverList(
            route,
            routeLeg,
            false
        ) as ManeuverResultV2.GetManeuverList
    }

    private fun processManeuverList(
        routeProgress: RouteProgress
    ): ManeuverResultV2.GetManeuverListWithProgress {
        return when (routeProgress.currentState) {
            INITIALIZED -> {
                // Always return the complete maneuver list as stored in maneuverState.allManeuvers
                getAllManeuverList(
                    routeProgress.route,
                    routeProgress.currentLegProgress?.routeLeg,
                    true
                ) as ManeuverResultV2.GetManeuverListWithProgress
            }
            TRACKING -> {
                val stepDistanceRemaining = getStepDistanceRemaining(routeProgress)
                return when (val currentBanner = routeProgress.bannerInstructions) {
                    null -> {
                        if (maneuverListWithProgress.isNotEmpty()) {
                            maneuverListWithProgress[0].distanceRemaining = stepDistanceRemaining
                            ManeuverResultV2.GetManeuverListWithProgress.Success(maneuverListWithProgress)
                        } else {
                            ManeuverResultV2.GetManeuverListWithProgress.Failure("")
                        }
                    }
                    else -> {
                        getManeuverListWith(routeProgress, currentBanner, stepDistanceRemaining)
                    }
                }
            }
            COMPLETE -> {
                Log.d("STATE", "state: COMPLETE")
                ManeuverResultV2.GetManeuverListWithProgress.Failure("")
            }
            OFF_ROUTE -> {
                Log.d("STATE", "state: OFF_ROUTE")
                ManeuverResultV2.GetManeuverListWithProgress.Failure("")
            }
            UNCERTAIN -> {
                Log.d("STATE", "state: UNCERTAIN")
                ManeuverResultV2.GetManeuverListWithProgress.Failure("")
            }
            else -> {
                ManeuverResultV2.GetManeuverListWithProgress.Failure("")
            }
        }
    }

    private fun getAllManeuverList(
        route: DirectionsRoute,
        routeLeg: RouteLeg? = null,
        isWithProgress: Boolean
    ): ManeuverResultV2 {
        if (!route.isSameRoute(maneuverState.route)) {
            maneuverState.route = route
            maneuverState.allManeuvers.clear()
            try {
                maneuverState.allManeuvers.addAll(buildManeuverList(route))
            } catch (exception: RuntimeException) {
                return if (!isWithProgress) {
                    ManeuverResultV2.GetManeuverList.Failure(exception.message)
                } else {
                    ManeuverResultV2.GetManeuverListWithProgress.Failure(exception.message)
                }
            }
        }
        return try {
            if (!isWithProgress) {
                ManeuverResultV2.GetManeuverList.Success(
                    getManeuverListFrom(maneuverState.allManeuvers, routeLeg)
                )
            } else {
                ManeuverResultV2.GetManeuverListWithProgress.Success(
                    getManeuverListFrom(maneuverState.allManeuvers, routeLeg)
                )
            }
        } catch (exception: RuntimeException) {
            if (!isWithProgress) {
                ManeuverResultV2.GetManeuverList.Failure(exception.message)
            } else {
                ManeuverResultV2.GetManeuverListWithProgress.Failure(exception.message)
            }
        }
    }

    private fun buildManeuverList(route: DirectionsRoute): List<LegToManeuvers> {
        val allManeuvers = mutableListOf<LegToManeuvers>()
        ifNonNull(route.legs()) { routeLegs ->
            routeLegs.forEach { routeLeg ->
                ifNonNull(routeLeg?.steps()) { legSteps ->
                    val stepList = mutableListOf<StepIndexToManeuvers>()
                    for (stepIndex in 0..legSteps.lastIndex) {
                        legSteps[stepIndex].bannerInstructions()?.let { bannerInstruction ->
                            val maneuverList = mutableListOf<ManeuverV2>()
                            if (bannerInstruction.isNotEmpty()) {
                                bannerInstruction.forEach { banner ->
                                    maneuverList.add(transformToManeuver(banner))
                                }
                                val stepIndexToManeuvers = StepIndexToManeuvers(
                                    stepIndex,
                                    maneuverList
                                )
                                stepList.add(stepIndexToManeuvers)
                            }
                        } ?: throw RuntimeException("LegStep should have valid banner instructions")
                    }
                    allManeuvers.add(LegToManeuvers(routeLeg, stepList))
                } ?: throw RuntimeException("RouteLeg should have valid steps")
            }
        } ?: throw RuntimeException("Route should have valid legs")
        if (allManeuvers.isEmpty()) { throw RuntimeException("Maneuver list could not be created") }
        return allManeuvers
    }

    private fun getManeuverListFrom(
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

    private fun getManeuverListWith(
        routeProgress: RouteProgress,
        bannerInstruction: BannerInstructions,
        stepDistanceRemaining: StepDistanceRemaining
    ): ManeuverResultV2.GetManeuverListWithProgress {
        val currentManeuver = transformToManeuver(bannerInstruction, routeProgress)
        val routeLeg = routeProgress.currentLegProgress?.routeLeg
        val stepIndex = routeProgress.currentLegProgress?.currentStepProgress?.stepIndex
        return ifNonNull(routeLeg, stepIndex) { leg, index ->
            maneuverState.allManeuvers.find {
                item -> item.routeLeg == leg
            }?.let { legToManeuver ->
                val stepToManeuverList = legToManeuver.stepIndexToManeuvers
                stepToManeuverList.find { item ->
                    item.stepIndex == index
                }?.let { legStep ->
                    maneuverListWithProgress.clear()
                    if (legStep.maneuverList.size == 1) {
                        // Update step distance remaining and return the maneuver at this index
                        legStep.maneuverList[0].distanceRemaining = stepDistanceRemaining
                        // Should we check if current maneuver is equal to maneuver at this index?
                    } else {
                        // Find the current maneuver in the list.
                        for (i in 0..legStep.maneuverList.lastIndex) {
                            if (currentManeuver.primary.text == legStep.maneuverList[i].primary.text) {
                                legStep.maneuverList[i].distanceRemaining = stepDistanceRemaining
                                break
                            }
                        }
                    }
                    stepToManeuverList.forEach { stepIndexToManeuver ->
                        maneuverListWithProgress.addAll(stepIndexToManeuver.maneuverList)
                    }
                    ManeuverResultV2.GetManeuverListWithProgress.Success(maneuverListWithProgress)
                } ?: ManeuverResultV2.GetManeuverListWithProgress.Failure(
                    "Could not find the $stepIndex"
                )
            } ?: ManeuverResultV2.GetManeuverListWithProgress.Failure(
                "Could not find the $routeLeg"
            )
        } ?: ManeuverResultV2.GetManeuverListWithProgress.Failure(
            "$routeLeg and $stepIndex in $routeProgress cannot be null"
        )
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

    /**
     * - maneuverListWithProgress should be synchronized?
     * - if a user invokes getManeuverList(route) and getManeuverList(routeProgress) then both
     *  the function first populates the entire list in maneuverstate.allManeuvers. Hence this
     *  should also be synchronized?
     * - getManeuverList(route) returns list with every item in the list containing total step distance
     * - getManeuverList(routeProgress) returns list with every item in the list containing total step distance
     * except the first one which contains step distance remaining. If the user maintains the same callback
     * which returns the list from both, how will the user distinguish?
     */
}
