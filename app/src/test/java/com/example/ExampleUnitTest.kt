package com.example

import com.example.data.local.OwnedCarEntity
import com.example.game.RacingSimulation
import com.example.model.RaceMode
import com.example.model.RacingCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `simulation accelerates after countdown and activates nitro`() {
        val car = RacingCatalog.cars.first()
        val owned = OwnedCarEntity(
            carId = car.id,
            isUnlocked = true,
            paintHex = car.defaultPaintHex
        )
        val driver = RacingCatalog.drivers.first()
        val track = RacingCatalog.tracks.first()

        val sim = RacingSimulation(
            mode = RaceMode.CITY_SPRINT,
            track = track,
            car = car,
            ownedCar = owned,
            driver = driver,
            onHapticEvent = {}
        )

        // Step past 3.2s countdown
        var snap = sim.step(0.016f, 0f, brakeInput = false, nitroInput = false)
        assertTrue(snap.isCountingDown)

        repeat(120) {
            snap = sim.step(0.05f, 0f, brakeInput = false, nitroInput = false)
        }
        assertFalse(snap.isCountingDown)
        assertTrue("Car should accelerate above 120 km/h, was ${snap.speedKmh}", snap.speedKmh > 120)

        // Activate nitro
        val nitroBefore = snap.nitroNorm
        repeat(10) {
            snap = sim.step(0.05f, 0f, brakeInput = false, nitroInput = true)
        }
        assertTrue(snap.isNitroActive)
        assertTrue(snap.nitroNorm < nitroBefore)
        assertEquals(6, snap.totalRacers)
    }
}
