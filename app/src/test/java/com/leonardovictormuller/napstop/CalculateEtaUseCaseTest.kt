package com.leonardovictormuller.napstop

import com.leonardovictormuller.napstop.domain.model.GeoPoint
import com.leonardovictormuller.napstop.domain.usecase.CalculateEtaUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class CalculateEtaUseCaseTest {

    private lateinit var useCase: CalculateEtaUseCase

    @Before
    fun setUp() {
        useCase = CalculateEtaUseCase()
    }

    @Test
    fun `haversine distance between same point is zero`() {
        val point = GeoPoint(-23.5614, -46.6557)
        val distance = useCase.haversineDistanceKm(point, point)
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun `haversine distance between Sao Paulo and Rio de Janeiro is approximately 357 km`() {
        val saoPaulo = GeoPoint(-23.5505, -46.6333)
        val rio = GeoPoint(-22.9068, -43.1729)
        val distance = useCase.haversineDistanceKm(saoPaulo, rio)
        // Known distance ~357 km, allowing 5 km tolerance
        assertTrue("Expected ~357 km, got $distance", abs(distance - 357.0) < 10.0)
    }

    @Test
    fun `invoke uses fallback speed when speed is zero`() {
        val current = GeoPoint(-23.5614, -46.6557)
        val destination = GeoPoint(-23.5500, -46.6500)
        val result = useCase(current = current, destination = destination, speedKmh = 0.0)
        assertEquals(CalculateEtaUseCase.FALLBACK_SPEED_KMH, result.effectiveSpeedKmh, 0.001)
    }

    @Test
    fun `invoke uses fallback speed when speed is very low`() {
        val current = GeoPoint(-23.5614, -46.6557)
        val destination = GeoPoint(-23.5500, -46.6500)
        val result = useCase(current = current, destination = destination, speedKmh = 0.1)
        assertEquals(CalculateEtaUseCase.FALLBACK_SPEED_KMH, result.effectiveSpeedKmh, 0.001)
    }

    @Test
    fun `invoke uses actual speed when speed is valid`() {
        val current = GeoPoint(-23.5614, -46.6557)
        val destination = GeoPoint(-23.5500, -46.6500)
        val result = useCase(current = current, destination = destination, speedKmh = 60.0)
        assertEquals(60.0, result.effectiveSpeedKmh, 0.001)
    }

    @Test
    fun `eta is zero when current equals destination`() {
        val point = GeoPoint(-23.5614, -46.6557)
        val result = useCase(current = point, destination = point, speedKmh = 30.0)
        assertEquals(0.0, result.etaMinutes, 0.001)
        assertEquals(0.0, result.distanceKm, 0.001)
    }

    @Test
    fun `eta calculation is correct for known distance and speed`() {
        // 30 km at 60 km/h = 30 minutes
        val current = GeoPoint(0.0, 0.0)
        val destination = GeoPoint(0.0, 0.2697) // ~30 km east at equator
        val result = useCase(current = current, destination = destination, speedKmh = 60.0)
        // Allow ±2 min tolerance due to Earth curvature approximation
        assertTrue("Expected ~30 min, got ${result.etaMinutes}", abs(result.etaMinutes - 30.0) < 2.0)
    }

    @Test
    fun `GeoPoint throws on invalid latitude`() {
        try {
            GeoPoint(latitude = 91.0, longitude = 0.0)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("latitude") == true)
        }
    }

    @Test
    fun `GeoPoint throws on invalid longitude`() {
        try {
            GeoPoint(latitude = 0.0, longitude = 181.0)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("longitude") == true)
        }
    }
}
