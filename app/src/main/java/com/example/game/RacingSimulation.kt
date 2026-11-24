package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.data.local.OwnedCarEntity
import com.example.model.CarModel
import com.example.model.CityTrack
import com.example.model.DriverProfile
import com.example.model.RaceMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

enum class TrafficType(val widthNorm: Float, val heightScale: Float) {
    SEDAN(0.24f, 0.72f),
    CITY_CAB(0.24f, 0.74f),
    SUV(0.28f, 0.90f),
    BOX_TRUCK(0.32f, 1.35f)
}

data class RoadSegment(
    val index: Int,
    val curve: Float,
    val elevation: Float,
    val buildingLeftHeight: Float,
    val buildingRightHeight: Float,
    val buildingLeftColor: Color,
    val buildingRightColor: Color,
    val hasStreetlight: Boolean,
    val hasOverpassArch: Boolean,
    val isFinishLine: Boolean
)

data class TrafficVehicle(
    val id: Int,
    var z: Float,
    var laneX: Float, // -0.78f .. 0.78f
    val speedMps: Float,
    val type: TrafficType,
    val bodyColor: Color,
    var nearMissedCooldown: Float = 0f
)

data class RivalCar(
    val id: Int,
    val name: String,
    var z: Float,
    var laneX: Float,
    var targetLaneX: Float,
    val baseSpeedMps: Float,
    val bodyColor: Color,
    val hasWing: Boolean
)

data class FloatingCallout(
    val id: Int,
    val text: String,
    val color: Color,
    var ageSec: Float = 0f
)

data class SmokeParticle(
    var xNorm: Float,
    var yOffset: Float,
    var size: Float,
    var alpha: Float
)

data class RaceSnapshot(
    val isCountingDown: Boolean,
    val countdownNumber: Int,
    val isFinished: Boolean,
    val isPaused: Boolean,
    val speedKmh: Int,
    val rpmNorm: Float,
    val gear: Int,
    val nitroNorm: Float,
    val isNitroActive: Boolean,
    val isDrifting: Boolean,
    val isBraking: Boolean,
    val currentLap: Int,
    val totalLaps: Int,
    val position: Int,
    val totalRacers: Int,
    val elapsedTimeMs: Long,
    val distanceKm: Float,
    val nearMisses: Int,
    val comboMultiplier: Int,
    val score: Int,
    val shieldRemaining: Int, // used in Endless Highway
    val topSpeedKmh: Int,
    val driverRadioText: String,
    val crashShake: Float,
    val playerX: Float,
    val playerZ: Float,
    val steerLean: Float
)

class RacingSimulation(
    val mode: RaceMode,
    val track: CityTrack,
    val car: CarModel,
    val ownedCar: OwnedCarEntity,
    val driver: DriverProfile,
    private val onHapticEvent: (heavy: Boolean) -> Unit
) {
    companion object {
        const val SEGMENT_LENGTH = 8f
        private val LANES = listOf(-0.72f, -0.24f, 0.24f, 0.72f)
    }

    private val segmentCount = (track.lapLengthMeters / SEGMENT_LENGTH).roundToInt().coerceAtLeast(180)
    val trackLengthMeters = segmentCount * SEGMENT_LENGTH
    val segments: List<RoadSegment>

    val traffic = mutableListOf<TrafficVehicle>()
    val rivals = mutableListOf<RivalCar>()
    val callouts = mutableListOf<FloatingCallout>()
    val smokeParticles = mutableListOf<SmokeParticle>()

    // Tuned car physics parameters
    private val topSpeedKmhEffective = (car.baseTopSpeedKmh +
        (ownedCar.engineLevel - 1) * 14 +
        (ownedCar.turboLevel - 1) * 6) * (1f + driver.topSpeedBonusPct)
    private val maxSpeedMps = topSpeedKmhEffective / 3.6f
    private val accelBase = (18.5f + (ownedCar.turboLevel - 1) * 2.8f +
        (1f / car.baseZeroToHundredSec) * 14f)
    private val handlingRate = (car.baseHandling + (ownedCar.handlingLevel - 1) * 0.045f) * 2.05f
    private val nitroCapacityBonus = 1f + (ownedCar.nitroLevel - 1) * 0.12f

    private var playerZ = 0f
    private var totalDistanceMeters = 0f
    private var playerX = 0.24f // start in lane 3
    private var speedMps = 0f
    private var nitro = 0.75f
    private var isNitroActive = false
    private var steerLean = 0f
    private var crashShake = 0f
    private var comboTimer = 0f
    private var comboMultiplier = 1
    private var nearMisses = 0
    private var topSpeedRecordedKmh = 0
    private var score = 0
    private var shieldRemaining = 3
    private var currentLap = 1
    private var elapsedRaceMs = 0L
    private var countdownRemainingSec = 3.2f
    private var lastCountdownDigit = 4
    private var isFinished = false
    private var isPaused = false
    private var radioText = "Engines hot. Watch the lights!"
    private var radioTimer = 4.0f
    private var nextCalloutId = 1

    init {
        val rand = Random(track.id.hashCode())
        val buildingPalette = listOf(
            Color(0xFF131C31),
            Color(0xFF19233C),
            Color(0xFF101829),
            Color(0xFF1F2B47)
        )
        segments = List(segmentCount) { i ->
            val phase = i.toFloat() / segmentCount.toFloat()
            val curve = (sin(phase * 2.0 * PI * 3.0) * 0.95 +
                cos(phase * 2.0 * PI * 5.0) * 0.45).toFloat() * track.curveIntensity
            val elev = (sin(phase * 2.0 * PI * 4.0) * 3.2).toFloat()
            RoadSegment(
                index = i,
                curve = curve,
                elevation = elev,
                buildingLeftHeight = if (i % 3 == 0) 24f + rand.nextFloat() * 44f else 0f,
                buildingRightHeight = if ((i + 1) % 3 == 0) 26f + rand.nextFloat() * 46f else 0f,
                buildingLeftColor = buildingPalette[i % buildingPalette.size],
                buildingRightColor = buildingPalette[(i + 2) % buildingPalette.size],
                hasStreetlight = i % 5 == 0,
                hasOverpassArch = i > 8 && i % 42 == 0,
                isFinishLine = i in 0..2
            )
        }

        spawnInitialTraffic(rand)
        if (mode == RaceMode.CITY_SPRINT) {
            spawnRivals()
        }
    }

    private fun spawnInitialTraffic(rand: Random) {
        val count = (22 * track.trafficDensity).roundToInt().coerceIn(12, 32)
        for (i in 0 until count) {
            val zPos = 140f + i * (trackLengthMeters / count)
            val lane = LANES[rand.nextInt(LANES.size)]
            val type = when (rand.nextInt(10)) {
                0, 1, 2 -> TrafficType.CITY_CAB
                3, 4 -> TrafficType.SUV
                5 -> TrafficType.BOX_TRUCK
                else -> TrafficType.SEDAN
            }
            val color = when (type) {
                TrafficType.CITY_CAB -> Color(0xFFF59E0B)
                TrafficType.BOX_TRUCK -> Color(0xFFE2E8F0)
                TrafficType.SUV -> listOf(Color(0xFF334155), Color(0xFF1E3A5F), Color(0xFF3F3F46)).random(rand)
                TrafficType.SEDAN -> listOf(Color(0xFF64748B), Color(0xFF991B1B), Color(0xFF0F766E), Color(0xFF475569)).random(rand)
            }
            traffic.add(
                TrafficVehicle(
                    id = i,
                    z = zPos % trackLengthMeters,
                    laneX = lane,
                    speedMps = (22f + rand.nextFloat() * 14f),
                    type = type,
                    bodyColor = color
                )
            )
        }
    }

    private fun spawnRivals() {
        val rivalSpecs = listOf(
            Triple("Vortex GT", Color(0xFF06B6D4), true),
            Triple("Mirage RS", Color(0xFFF59E0B), true),
            Triple("Kuro V8", Color(0xFF8B5CF6), false),
            Triple("Strada R", Color(0xFF10B981), true),
            Triple("Volt Spec", Color(0xFFE2E8F0), false)
        )
        rivalSpecs.forEachIndexed { idx, (name, color, wing) ->
            rivals.add(
                RivalCar(
                    id = idx + 1,
                    name = name,
                    z = 35f + idx * 24f,
                    laneX = LANES[idx % LANES.size],
                    targetLaneX = LANES[idx % LANES.size],
                    baseSpeedMps = maxSpeedMps * (0.76f + idx * 0.042f),
                    bodyColor = color,
                    hasWing = wing
                )
            )
        }
    }

    fun setPaused(paused: Boolean) {
        isPaused = paused
    }

    fun step(
        dtSecRaw: Float,
        steerInput: Float, // -1f..1f
        brakeInput: Boolean,
        nitroInput: Boolean
    ): RaceSnapshot {
        val dt = dtSecRaw.coerceIn(0.001f, 0.05f)

        if (isPaused || isFinished) {
            return buildSnapshot(brakeInput, isDrifting = false)
        }

        // Countdown phase
        if (countdownRemainingSec > 0f) {
            countdownRemainingSec -= dt
            val digit = kotlin.math.ceil(countdownRemainingSec).toInt().coerceAtLeast(1)
            if (digit != lastCountdownDigit && digit <= 3) {
                lastCountdownDigit = digit
                onHapticEvent(false)
            }
            if (countdownRemainingSec <= 0f) {
                radioText = "Green light! Floor it!"
                radioTimer = 2.6f
                onHapticEvent(true)
            }
            return buildSnapshot(isBraking = false, isDrifting = false)
        }

        elapsedRaceMs += (dt * 1000f).toLong()
        if (radioTimer > 0f) radioTimer -= dt
        if (crashShake > 0f) crashShake = (crashShake - dt * 2.8f).coerceAtLeast(0f)
        if (comboTimer > 0f) {
            comboTimer -= dt
            if (comboTimer <= 0f) comboMultiplier = 1
        }

        // Nitro activation
        if (nitroInput && nitro > 0.06f && !brakeInput) {
            if (!isNitroActive) {
                onHapticEvent(false)
                setRadio("Nitro engaged!")
            }
            isNitroActive = true
        } else if (!nitroInput || nitro <= 0.01f || brakeInput) {
            isNitroActive = false
        }

        if (isNitroActive) {
            nitro = (nitro - dt * (0.28f / nitroCapacityBonus)).coerceAtLeast(0f)
        }

        // Endless mode speed scaling over distance
        val endlessBoost = if (mode == RaceMode.ENDLESS_HIGHWAY) {
            1f + (totalDistanceMeters / 8000f).coerceAtMost(0.28f)
        } else 1f

        val targetMaxSpeed = maxSpeedMps * endlessBoost * (if (isNitroActive) 1.24f else 1.0f)

        // Acceleration & Braking
        if (brakeInput) {
            speedMps = (speedMps - 52f * dt).coerceAtLeast(10f)
        } else {
            val speedRatio = (speedMps / targetMaxSpeed).coerceIn(0f, 1f)
            val effectiveAccel = accelBase * (1.15f - speedRatio * 0.72f) *
                if (isNitroActive) 1.75f else 1.0f
            if (speedMps < targetMaxSpeed) {
                speedMps = (speedMps + effectiveAccel * dt).coerceAtMost(targetMaxSpeed)
            } else {
                speedMps = (speedMps - 18f * dt).coerceAtLeast(targetMaxSpeed)
            }
        }

        // Steering & Centrifugal Curve Pull
        val currentSegIdx = segmentIndexAt(playerZ)
        val currentSeg = segments[currentSegIdx]
        val speedFactor = (speedMps / maxSpeedMps).coerceIn(0.25f, 1.15f)
        val centrifugal = currentSeg.curve * speedFactor * speedFactor * 0.42f

        val steerDelta = steerInput * handlingRate * speedFactor * dt
        playerX = (playerX + steerDelta - centrifugal * dt).coerceIn(-1.12f, 1.12f)
        steerLean += (steerInput - steerLean) * (dt * 12f).coerceAtMost(1f)

        // Shoulder rumble slowdown
        if (abs(playerX) > 0.96f && speedMps > 24f) {
            speedMps -= 24f * dt
            crashShake = maxOf(crashShake, 0.16f)
        }

        // Drift detection & Nitro recharge
        val isDrifting = abs(steerInput) > 0.65f && speedMps > 34f && !brakeInput
        if (isDrifting) {
            val chargeRate = 0.11f * driver.driftChargeMultiplier
            nitro = (nitro + chargeRate * dt).coerceAtMost(1f)
            if (smokeParticles.size < 28) {
                smokeParticles.add(
                    SmokeParticle(
                        xNorm = playerX + (if (steerInput > 0) -0.10f else 0.10f),
                        yOffset = 0f,
                        size = 10f + Random.nextFloat() * 8f,
                        alpha = 0.55f
                    )
                )
            }
        } else {
            // Passive gentle regen
            nitro = (nitro + 0.015f * dt).coerceAtMost(1f)
        }

        // Advance smoke particles
        val smokeIter = smokeParticles.iterator()
        while (smokeIter.hasNext()) {
            val p = smokeIter.next()
            p.yOffset += dt * 45f
            p.size += dt * 24f
            p.alpha -= dt * 1.15f
            if (p.alpha <= 0f) smokeIter.remove()
        }

        // Advance callouts
        val callIter = callouts.iterator()
        while (callIter.hasNext()) {
            val c = callIter.next()
            c.ageSec += dt
            if (c.ageSec > 1.25f) callIter.remove()
        }

        // Advance player position & laps
        val dz = speedMps * dt
        val prevZ = playerZ
        playerZ += dz
        totalDistanceMeters += dz

        if (playerZ >= trackLengthMeters) {
            playerZ -= trackLengthMeters
            if (mode != RaceMode.ENDLESS_HIGHWAY) {
                if (currentLap >= mode.laps) {
                    isFinished = true
                    setRadio("Checkered flag! Incredible run!")
                } else {
                    currentLap++
                    val lapMsg = if (currentLap == mode.laps) "Final Lap — push to the limit!" else "Lap $currentLap/${mode.laps}"
                    setRadio(lapMsg)
                    addCallout("LAP $currentLap", Color(0xFF06B6D4))
                }
            } else {
                currentLap++
            }
        }

        // Score & Top speed tracking
        val currentKmh = (speedMps * 3.6f).roundToInt()
        if (currentKmh > topSpeedRecordedKmh) topSpeedRecordedKmh = currentKmh
        score += ((dz * 0.8f) * comboMultiplier).roundToInt()

        // Update AI Rivals
        if (mode == RaceMode.CITY_SPRINT) {
            for (rival in rivals) {
                val distGap = rival.z - totalDistanceMeters
                // Subtle rubber-banding so races stay competitive
                val rubberBand = when {
                    distGap > 180f -> 0.88f
                    distGap < -140f -> 1.14f
                    else -> 1.0f
                }
                rival.z += rival.baseSpeedMps * rubberBand * dt
                if (abs(rival.laneX - rival.targetLaneX) < 0.03f && Random.nextFloat() < 0.02f) {
                    rival.targetLaneX = LANES.random()
                }
                rival.laneX += (rival.targetLaneX - rival.laneX) * (dt * 2.5f)
            }
        }

        // Update Traffic & Collisions / Near-Misses
        for (carObj in traffic) {
            if (carObj.nearMissedCooldown > 0f) carObj.nearMissedCooldown -= dt
            carObj.z += carObj.speedMps * dt
            if (carObj.z >= trackLengthMeters) carObj.z -= trackLengthMeters

            val relZ = wrappedDeltaZ(playerZ, carObj.z)
            if (relZ in -3.5f..9.5f) {
                val xDist = abs(playerX - carObj.laneX)
                val hitThreshold = (carObj.type.widthNorm * 0.5f) + 0.14f
                val nearMissThreshold = hitThreshold + 0.18f

                if (xDist < hitThreshold && relZ in -2.5f..6.2f) {
                    // Collision!
                    speedMps = (speedMps * 0.42f).coerceAtLeast(16f)
                    isNitroActive = false
                    crashShake = 1.0f
                    comboMultiplier = 1
                    comboTimer = 0f
                    carObj.z = (playerZ + 220f) % trackLengthMeters
                    onHapticEvent(true)
                    addCallout("IMPACT!", Color(0xFFEF4444))
                    setRadio("Watch the traffic!")

                    if (mode == RaceMode.ENDLESS_HIGHWAY) {
                        shieldRemaining--
                        if (shieldRemaining <= 0) {
                            isFinished = true
                            setRadio("Chassis totaled! Great highway run!")
                        }
                    }
                } else if (xDist < nearMissThreshold && carObj.nearMissedCooldown <= 0f && currentKmh >= 135) {
                    carObj.nearMissedCooldown = 2.0f
                    nearMisses++
                    comboMultiplier = (comboMultiplier + 1).coerceAtMost(5)
                    comboTimer = 4.5f
                    val pts = 120 * comboMultiplier
                    score += pts
                    nitro = (nitro + 0.12f * driver.driftChargeMultiplier).coerceAtMost(1f)
                    onHapticEvent(false)
                    addCallout("NEAR MISS! x$comboMultiplier", Color(0xFFFBBF24))
                    if (comboMultiplier >= 3) {
                        setRadio("Insane weave! x$comboMultiplier combo!")
                    }
                }
            }
        }

        return buildSnapshot(brakeInput, isDrifting)
    }

    fun wrappedDeltaZ(fromZ: Float, toZ: Float): Float {
        var d = toZ - fromZ
        if (d < -trackLengthMeters * 0.5f) d += trackLengthMeters
        if (d > trackLengthMeters * 0.5f) d -= trackLengthMeters
        return d
    }

    fun segmentIndexAt(z: Float): Int {
        val wrapped = ((z % trackLengthMeters) + trackLengthMeters) % trackLengthMeters
        return ((wrapped / SEGMENT_LENGTH).toInt()).coerceIn(0, segmentCount - 1)
    }

    private fun setRadio(text: String) {
        if (radioTimer <= 1.2f) {
            radioText = text
            radioTimer = 2.8f
        }
    }

    private fun addCallout(text: String, color: Color) {
        callouts.add(FloatingCallout(id = nextCalloutId++, text = text, color = color))
    }

    private fun calculatePosition(): Int {
        if (mode == RaceMode.TIME_ATTACK) {
            return when {
                elapsedRaceMs <= track.goldTimeMs -> 1
                elapsedRaceMs <= track.silverTimeMs -> 2
                else -> 3
            }
        }
        if (mode == RaceMode.ENDLESS_HIGHWAY) return 1
        var ahead = 0
        for (rival in rivals) {
            if (rival.z > totalDistanceMeters) ahead++
        }
        return ahead + 1
    }

    private fun buildSnapshot(isBraking: Boolean, isDrifting: Boolean): RaceSnapshot {
        val speedKmh = (speedMps * 3.6f).roundToInt()
        val speedRatio = (speedMps / maxSpeedMps).coerceIn(0f, 1.25f)
        val gear = when {
            speedKmh < 35 -> 1
            speedKmh < 80 -> 2
            speedKmh < 135 -> 3
            speedKmh < 195 -> 4
            speedKmh < 260 -> 5
            else -> 6
        }
        val rpmNorm = ((speedRatio * 6f) % 1f).let {
            if (speedKmh < 10) 0.15f else (0.35f + it * 0.62f).coerceIn(0.2f, 0.98f)
        }
        return RaceSnapshot(
            isCountingDown = countdownRemainingSec > 0f,
            countdownNumber = kotlin.math.ceil(countdownRemainingSec).toInt().coerceIn(1, 3),
            isFinished = isFinished,
            isPaused = isPaused,
            speedKmh = speedKmh,
            rpmNorm = rpmNorm,
            gear = gear,
            nitroNorm = nitro,
            isNitroActive = isNitroActive,
            isDrifting = isDrifting,
            isBraking = isBraking,
            currentLap = currentLap.coerceAtMost(mode.laps),
            totalLaps = mode.laps,
            position = calculatePosition(),
            totalRacers = if (mode == RaceMode.CITY_SPRINT) rivals.size + 1 else 1,
            elapsedTimeMs = elapsedRaceMs,
            distanceKm = totalDistanceMeters / 1000f,
            nearMisses = nearMisses,
            comboMultiplier = comboMultiplier,
            score = score,
            shieldRemaining = shieldRemaining,
            topSpeedKmh = topSpeedRecordedKmh,
            driverRadioText = radioText,
            crashShake = crashShake,
            playerX = playerX,
            playerZ = playerZ,
            steerLean = steerLean
        )
    }

    fun calculateCreditsEarned(position: Int): Int {
        val base = when (mode) {
            RaceMode.CITY_SPRINT -> when (position) {
                1 -> 650
                2 -> 420
                3 -> 280
                else -> 160
            }
            RaceMode.TIME_ATTACK -> when (position) {
                1 -> 600
                2 -> 380
                else -> 200
            }
            RaceMode.ENDLESS_HIGHWAY -> (totalDistanceMeters / 12f).roundToInt().coerceAtLeast(120)
        }
        return base + nearMisses * 35
    }
}
