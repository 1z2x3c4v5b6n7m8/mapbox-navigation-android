package com.mapbox.navigation.ui.maps.internal.route.line

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.ClosestRouteValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteNotFound
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Extension functions for [MapboxRouteLineApi] calls that are implemented as callbacks. This offers
 * an alternative to those callbacks by providing Kotlin oriented suspend functions.
 */
object MapboxRouteLineApiExtensions {

    /**
     * Sets the routes that will be operated on.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     *
     * @return a state which contains the side effects to be applied to the map
     */
    suspend fun MapboxRouteLineApi.setRoutes(newRoutes: List<RouteLine>):
        Expected<RouteLineError, RouteSetValue> {
        return suspendCoroutine { continuation ->
            this.setRoutes(
                newRoutes
            ) { value -> continuation.resume(value) }
        }
    }

    /**
     * @return a state which contains the side effects to be applied to the map. The data
     * can be used to draw the current route line(s) on the map.
     */
    suspend fun MapboxRouteLineApi.getRouteDrawData(): Expected<RouteLineError, RouteSetValue> {
        return suspendCoroutine { continuation ->
            this.getRouteDrawData { value -> continuation.resume(value) }
        }
    }

    /**
     * The map will be queried for a route line feature at the target point or a bounding box
     * centered at the target point with a padding value determining the box's size. If a route
     * feature is found the index of that route in this class's route collection is returned. The
     * primary route is given precedence if more than one route is found.
     *
     * @param target a target latitude/longitude serving as the search point
     * @param mapboxMap a reference to the [MapboxMap] that will be queried
     * @param padding a sizing value added to all sides of the target point for creating a bounding
     * box to search in.
     *
     * @return a value containing the [DirectionsRoute] found or an error indicating no route was
     * found.
     */
    suspend fun MapboxRouteLineApi.findClosestRoute(
        target: Point,
        mapboxMap: MapboxMap,
        padding: Float,
    ): Expected<RouteNotFound, ClosestRouteValue> {
        return suspendCoroutine { continuation ->
            this.findClosestRoute(
                target,
                mapboxMap,
                padding
            ) { value -> continuation.resume(value) }
        }
    }

    /**
     * Clears the route line data.
     *
     * @return a state representing the side effects to be rendered on the map. In this case
     * the map should appear without any route lines.
     */
    suspend fun MapboxRouteLineApi.clearRouteLine(): Expected<RouteLineError, RouteLineClearValue> {
        return suspendCoroutine { continuation ->
            this.clearRouteLine { value -> continuation.resume(value) }
        }
    }

    /**
     * Adjusts the route line visibility so that only the current route leg is visible. This is
     * intended to be used with routes that have multiple waypoints.
     *
     * Your activity or fragment should register a route progress listener and on each
     * route progress call this method and render the result using the [MapboxRouteLineView].
     *
     * This method should NOT be used in conjunction with the vanishing route line feature. If
     * using the vanishing route line feature ignore this method. The normal vanishing route
     * line mechanism will take care of hiding inactive route legs if the diminish inactive
     * route legs option is enabled AND the vanishing route line feature is enabled.
     *
     * @param routeProgress a [RouteProgress]
     */
    suspend fun MapboxRouteLineApi.showOnlyActiveLeg(routeProgress: RouteProgress):
        Expected<RouteLineError, RouteLineUpdateValue> {
        return suspendCoroutine { continuation ->
            this.showOnlyActiveLeg(routeProgress) { value ->
                continuation.resume(value)
            }
        }
    }
}
