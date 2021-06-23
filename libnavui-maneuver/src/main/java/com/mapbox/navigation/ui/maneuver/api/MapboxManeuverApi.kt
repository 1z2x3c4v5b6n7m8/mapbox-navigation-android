package com.mapbox.navigation.ui.maneuver.api

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.maneuver.ManeuverAction
import com.mapbox.navigation.ui.maneuver.ManeuverProcessor
import com.mapbox.navigation.ui.maneuver.ManeuverResult
import com.mapbox.navigation.ui.maneuver.ManeuverState
import com.mapbox.navigation.ui.maneuver.RoadShieldContentManager
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.launch

class MapboxManeuverApi internal constructor(
    private val distanceFormatter: DistanceFormatter,
    private val processor: ManeuverProcessor
) {

    private val mainJobController: JobControl by lazy { ThreadController.getMainScopeAndRootJob() }
    private val maneuverState = ManeuverState()
    private val roadShieldContentManager = RoadShieldContentManager()

    /**
     * @param formatter contains various instances for use in formatting distance related data
     * for display in the UI
     *
     * @return a [MapboxManeuverApi]
     */
    constructor(formatter: DistanceFormatter) : this(formatter, ManeuverProcessor)

    /**
     * Given a [DirectionsRoute] the function iterates through all the [RouteLeg] in a [DirectionsRoute].
     * For every [RouteLeg] the API iterates through all the [LegStep]. For every [LegStep] the API
     * parses the [BannerInstructions], transforms it into [Maneuver] object and adds it to a list.
     * Once this list is ready the API maps this list to the [RouteLeg].
     *
     * @param route DirectionsRoute The route associated
     * @param routeLeg RouteLeg Specify to inform the API of the [RouteLeg] you wish to get the list of [Maneuver].
     * If null, the API returns the list of maneuvers for the first [RouteLeg] in a [DirectionsRoute]
     * @param callback ManeuverCallback invoked with appropriate result
     */
    fun getManeuvers(
        route: DirectionsRoute,
        routeLeg: RouteLeg? = null,
        callback: ManeuverCallback,
    ) {
        val action = ManeuverAction.GetManeuverListWithRoute(
            route,
            routeLeg,
            maneuverState,
            distanceFormatter
        )
        when (val result = processor.process(action) as ManeuverResult.GetManeuverList) {
            is ManeuverResult.GetManeuverList.Success -> {
                val allManeuvers = result.maneuvers
                callback.onManeuvers(ExpectedFactory.createValue(allManeuvers))
            }
            is ManeuverResult.GetManeuverList.Failure -> {
                callback.onError(ExpectedFactory.createError(ManeuverError(result.error)))
            }
            else -> {
                callback.onError(
                    ExpectedFactory.createError(
                        ManeuverError(
                            "Inappropriate  $result emitted for $action.", null
                        )
                    )
                )
            }
        }
    }

    /**
     * Given [RouteProgress] the function prepares a list of [Maneuver] and returns the list with the
     * first maneuver holding step distance remaining data and other subsequent maneuvers holding the
     * total step distance.
     *
     * @param routeProgress RouteProgress
     * @param callback ManeuverCallback invoked with appropriate result
     */
    fun getManeuvers(
        routeProgress: RouteProgress,
        callback: ManeuverCallback
    ) {
        val action = ManeuverAction.GetManeuverList(routeProgress, maneuverState, distanceFormatter)
        when (
            val result = processor.process(action) as
                ManeuverResult.GetManeuverListWithProgress
        ) {
            is ManeuverResult.GetManeuverListWithProgress.Success -> {
                val allManeuvers = result.maneuvers
                callback.onManeuvers(ExpectedFactory.createValue(allManeuvers))
            }
            is ManeuverResult.GetManeuverListWithProgress.Failure -> {
                callback.onError(ExpectedFactory.createError(ManeuverError(result.error)))
            }
            else -> {
                callback.onError(
                    ExpectedFactory.createError(
                        ManeuverError(
                            "Inappropriate  $result emitted for $action.", null
                        )
                    )
                )
            }
        }
    }

    // TODO: The view needs to maintain the Map<String, RoadShield> so that it doesn't blink.
    // TODO: Write unit tests
    // TODO: Test off-route
    /**
     * Given a list of [Maneuver] the function iterates through the list starting at startIndex and
     * ending at endIndex to request shields for urls associated in [RoadShieldComponentNode]. If
     * not specified, startIndex is 0 and endIndex is the last index in the list.
     *
     * @param maneuvers list of maneuvers
     * @param callback RoadShieldCallback invoked with appropriate result
     * @param startIndex starting position of item in the list
     * @param endIndex end position of item in the list
     */
    @JvmOverloads
    fun getRoadShields(
        maneuvers: List<Maneuver>,
        callback: RoadShieldCallback,
        startIndex: Int = 0,
        endIndex: Int = maneuvers.lastIndex
    ) {
        mainJobController.scope.launch {
            val result = roadShieldContentManager.getShields(
                startIndex,
                endIndex,
                maneuvers
            )
            callback.onRoadShields(maneuvers, result.shields, result.errors)
        }
    }

    /**
     * Invoke the function to cancel any job invoked through other APIs
     */
    fun cancel() {
        roadShieldContentManager.cancel()
        mainJobController.job.children.forEach {
            it.cancel()
        }
    }
}
