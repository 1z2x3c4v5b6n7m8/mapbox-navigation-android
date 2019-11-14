package com.mapbox.navigation.base.route.model

data class Route(
    val routeIndex: String?,
    val distance: Double?,
    val duration: Long?,
    val geometry: String?,
    val weight: Double?,
    val weightName: String?,
    val legs: RouteLegsNavigation?,
    val voiceLanguage: String?
)

data class RouteLegsNavigation(
    val legs: List<*>
)