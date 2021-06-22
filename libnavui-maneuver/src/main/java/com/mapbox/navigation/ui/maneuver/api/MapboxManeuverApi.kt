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
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MapboxManeuverApi internal constructor(
    private val distanceFormatter: DistanceFormatter,
    private val processor: ManeuverProcessor
) {

    private val mainJobController: JobControl by lazy { ThreadController.getMainScopeAndRootJob() }
    private var routeShieldJob: Job? = null

    /**
     * @param formatter contains various instances for use in formatting distance related data
     * for display in the UI
     *
     * @return a [MapboxManeuverApi]
     */
    constructor(formatter: DistanceFormatter) : this(formatter, ManeuverProcessor())

    /**
     * Given a [DirectionsRoute] the function iterates through all the [RouteLeg]. For every [RouteLeg]
     * the API iterates through all the [LegStep]. For every [LegStep] the API parses the [BannerInstructions],
     * transforms it into [Maneuver] object and adds it to a list. Once this list is ready the API maps this
     * list to the [RouteLeg].
     *
     * @param route DirectionsRoute The route associated
     * @param routeLeg RouteLeg Specify to inform the API of the [RouteLeg] you wish to get the list of [Maneuver].
     * If null, the API returns the list of maneuvers for the first [RouteLeg] in a [DirectionsRoute]
     * @param callback ManeuverCallbackV2 invoked with appropriate result
     */
    fun getManeuverList(
            route: DirectionsRoute,
            routeLeg: RouteLeg? = null,
            callback: ManeuverCallback,
    ) {
        val action = ManeuverAction.GetManeuverListWithRoute(route, routeLeg, distanceFormatter)
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
     * Given [RouteProgress] the function extracts the current banner instruction. Using the
     * list created and stored with [setManeuversWith], the API finds the position where the
     * current banner instruction is contained in the list. Starting at that position, the
     * function returns a sub list of maneuver instructions.
     */
    fun getManeuverList(
        routeProgress: RouteProgress,
        callback: ManeuverCallback
    ) {
        val action = ManeuverAction.GetManeuverList(routeProgress, distanceFormatter)
        when (val result = processor.process(action) as
            ManeuverResult.GetManeuverListWithProgress) {
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

    // The view needs to maintain the Map<String, RoadShield> so that it doesn't blink.
    // The above change will have to be done on the view. The API already sends the map through
    // the callback.
    // TODO: Move the state from Processor to API
    // TODO: Write unit tests
    // TODO: Remove legacy maneuver classes
    // TODO: Test off-route, multiLeg and other geographies
    fun getRoadShields(
        maneuvers: List<Maneuver>,
        callback: RoadShieldCallback,
        startIndex: Int = 0,
        endIndex: Int = maneuvers.lastIndex
    ) {
        if (routeShieldJob == null ||
            (routeShieldJob != null && routeShieldJob!!.isCompleted && !routeShieldJob!!.isActive)
        ) {
            routeShieldJob = mainJobController.scope.launch {
                when (val result = processor.processRoadShields(startIndex, endIndex, maneuvers)) {
                    is ManeuverResult.GetRoadShields.Success -> {
                        callback.onRoadShields(ExpectedFactory.createValue(result.roadShields))
                    }
                    is ManeuverResult.GetRoadShields.Failure -> {
                        callback.onRoadShields(
                            ExpectedFactory.createError(ManeuverError(result.error))
                        )
                    }
                }
            }
        }
    }
}
