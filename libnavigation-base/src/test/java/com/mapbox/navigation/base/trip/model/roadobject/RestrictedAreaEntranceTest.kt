package com.mapbox.navigation.base.trip.model.roadobject

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.roadobject.restrictedarea.RestrictedAreaEntrance
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class RestrictedAreaEntranceTest :
    BuilderTest<RestrictedAreaEntrance, RestrictedAreaEntrance.Builder>() {

    override fun getImplementationClass() = RestrictedAreaEntrance::class

    override fun getFilledUpBuilder() = RestrictedAreaEntrance.Builder(
        RoadObjectGeometry.Builder(
            456.0,
            Point.fromLngLat(10.0, 20.0),
            1,
            2
        ).build()
    ).distanceFromStartOfRoute(123.0)

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }

    @Test
    fun `distanceFromStartOfRoute is null if negative value passed`() {
        val areaEntrance = RestrictedAreaEntrance.Builder(mockk())
            .distanceFromStartOfRoute(-1.0)
            .build()

        assertEquals(null, areaEntrance.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute is null if null passed`() {
        val areaEntrance = RestrictedAreaEntrance.Builder(mockk())
            .distanceFromStartOfRoute(null)
            .build()

        assertEquals(null, areaEntrance.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute not null if positive value passed`() {
        val areaEntrance = RestrictedAreaEntrance.Builder(mockk())
            .distanceFromStartOfRoute(1.0)
            .build()

        assertEquals(1.0, areaEntrance.distanceFromStartOfRoute)
    }
}
