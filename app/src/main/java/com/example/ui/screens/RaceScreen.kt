package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.RaceAudioEngine
import com.example.game.RaceCanvas
import com.example.game.RaceSnapshot
import com.example.game.RacingSimulation
import com.example.model.RaceMode
import com.example.model.SteeringMode
import com.example.ui.MainUiState
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.ChampionshipAmber
import com.example.ui.theme.DangerRed
import com.example.ui.theme.NitroCyan
import com.example.ui.theme.PitSurface
import com.example.ui.theme.PitSurfaceElevated
import com.example.ui.theme.RacingCrimson
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TelemetryEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RaceScreen(
    state: MainUiState,
    onSaveRaceResult: suspend (
        mode: RaceMode,
        position: Int,
        elapsedMs: Long,
        distanceKm: Float,
        topSpeed: Int,
        nearMisses: Int,
        score: Int,
        credits: Int
    ) -> Long,
    onDoubleCreditsAd: (Activity?, Int, Long?, () -> Unit) -> Unit,
    onExitToPit: (Activity?) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioEngine = remember { RaceAudioEngine(context) }

    var raceRestartSeed by remember { mutableIntStateOf(0) }
    val simulation = remember(raceRestartSeed) {
        RacingSimulation(
            mode = state.selectedMode,
            track = state.selectedTrack,
            car = state.selectedCar,
            ownedCar = state.selectedOwnedCar,
            driver = state.selectedDriver,
            onHapticEvent = { heavy ->
                audioEngine.pulseHaptic(state.settings.hapticsEnabled, heavy)
            }
        )
    }

    var snapshot by remember(raceRestartSeed) {
        mutableStateOf(simulation.step(0.016f, 0f, brakeInput = false, nitroInput = false))
    }
    var frameTick by remember { mutableLongStateOf(0L) }
    var isPaused by remember(raceRestartSeed) { mutableStateOf(false) }

    // Continuous Input States
    var leftPressed by remember { mutableStateOf(false) }
    var rightPressed by remember { mutableStateOf(false) }
    var brakePressed by remember { mutableStateOf(false) }
    var nitroPressed by remember { mutableStateOf(false) }
    var dragSteerValue by remember { mutableFloatStateOf(0f) }
    var tiltSteerValue by remember { mutableFloatStateOf(0f) }

    // Post-race persistence state
    var savedRecordId by remember(raceRestartSeed) { mutableStateOf<Long?>(null) }
    var earnedCredits by remember(raceRestartSeed) { mutableIntStateOf(0) }
    var adDoubled by remember(raceRestartSeed) { mutableStateOf(false) }

    // Hardware Tilt Accelerometer Listener when TILT_SENSOR is active
    DisposableEffect(state.settings.steeringMode, isPaused) {
        if (state.settings.steeringMode == SteeringMode.TILT_SENSOR && !isPaused) {
            val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    // Invert X axis so tilting phone left steers left
                    val raw = (-event.values[0] / 4.2f).coerceIn(-1f, 1f)
                    tiltSteerValue = if (kotlin.math.abs(raw) < 0.08f) 0f else raw
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            if (accel != null) {
                sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
            }
            onDispose {
                sm.unregisterListener(listener)
            }
        } else {
            tiltSteerValue = 0f
            onDispose {}
        }
    }

    // Engine Audio Lifecycle
    DisposableEffect(Unit) {
        audioEngine.startEngine(state.settings.soundEnabled)
        onDispose {
            audioEngine.stopEngine()
        }
    }

    // Pause on System Back during active racing
    BackHandler(enabled = !snapshot.isFinished) {
        isPaused = !isPaused
        simulation.setPaused(isPaused)
    }

    // 60 FPS Main Race Loop
    LaunchedEffect(raceRestartSeed) {
        var lastNanos = withFrameNanos { it }
        while (true) {
            val nowNanos = withFrameNanos { it }
            val dt = ((nowNanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            lastNanos = nowNanos

            simulation.setPaused(isPaused)
            val steer = when (state.settings.steeringMode) {
                SteeringMode.TOUCH_BUTTONS -> (if (rightPressed) 1f else 0f) - (if (leftPressed) 1f else 0f)
                SteeringMode.DRAG_STEER -> dragSteerValue
                SteeringMode.TILT_SENSOR -> tiltSteerValue
            }
            val nextSnap = simulation.step(
                dtSecRaw = dt,
                steerInput = steer,
                brakeInput = brakePressed,
                nitroInput = nitroPressed
            )
            snapshot = nextSnap
            frameTick++

            val shouldPlaySound = state.settings.soundEnabled && !isPaused && !nextSnap.isFinished
            audioEngine.updateTelemetry(
                speedRatio = (nextSnap.speedKmh / 360f).coerceIn(0f, 1f),
                nitro = nextSnap.isNitroActive,
                soundEnabled = shouldPlaySound
            )

            if (nextSnap.isFinished && savedRecordId == null) {
                val credits = simulation.calculateCreditsEarned(nextSnap.position)
                earnedCredits = credits
                savedRecordId = onSaveRaceResult(
                    state.selectedMode,
                    nextSnap.position,
                    nextSnap.elapsedTimeMs,
                    nextSnap.distanceKm,
                    nextSnap.topSpeedKmh,
                    nextSnap.nearMisses,
                    nextSnap.score,
                    credits
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(CarbonBlack)) {
        // 1. 3D City Highway Canvas
        RaceCanvas(
            simulation = simulation,
            snapshot = snapshot,
            carColor = Color(state.selectedOwnedCar.paintHex),
            carModel = state.selectedCar,
            track = state.selectedTrack,
            frameTick = frameTick,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (state.settings.steeringMode == SteeringMode.DRAG_STEER) {
                        Modifier.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = { dragSteerValue = 0f },
                                onDragCancel = { dragSteerValue = 0f },
                                onHorizontalDrag = { _, dragAmount ->
                                    dragSteerValue = (dragSteerValue + dragAmount / 120f).coerceIn(-1f, 1f)
                                }
                            )
                        }
                    } else Modifier
                )
        )

        // 2. Top Motorsport HUD + Real Human Driver Radio Comms
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Real Human Driver Live Comms Pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CarbonBlack.copy(alpha = 0.78f),
                    border = BorderStroke(1.dp, NitroCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = state.selectedDriver.portraitRes),
                            contentDescription = state.selectedDriver.name,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(1.dp, NitroCyan, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Column {
                            Text(
                                text = state.selectedDriver.name.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = NitroCyan
                            )
                            Text(
                                text = snapshot.driverRadioText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Race Position / Lap / Endless Shield + Pause
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CarbonBlack.copy(alpha = 0.78f),
                    border = BorderStroke(1.dp, SubtleBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (state.selectedMode == RaceMode.ENDLESS_HIGHWAY) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("SCORE ${snapshot.score}", style = MaterialTheme.typography.labelSmall, color = ChampionshipAmber)
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    repeat(3) { idx ->
                                        Icon(
                                            imageVector = Icons.Rounded.Favorite,
                                            contentDescription = null,
                                            tint = if (idx < snapshot.shieldRemaining) RacingCrimson else TextSecondary.copy(alpha = 0.3f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (state.selectedMode == RaceMode.CITY_SPRINT) {
                                        "P${snapshot.position}/${snapshot.totalRacers}  •  LAP ${snapshot.currentLap}/${snapshot.totalLaps}"
                                    } else {
                                        "LAP ${snapshot.currentLap}/${snapshot.totalLaps}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ChampionshipAmber
                                )
                                Text(
                                    text = formatRaceTime(snapshot.elapsedTimeMs),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextPrimary
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                isPaused = true
                                simulation.setPaused(true)
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(36.dp)
                                .testTag("pause_race_button")
                        ) {
                            Icon(Icons.Rounded.Pause, contentDescription = "Pause Race", tint = TextPrimary)
                        }
                    }
                }
            }

            // Floating Callouts (Near Miss / Combo)
            simulation.callouts.lastOrNull()?.let { callout ->
                Surface(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(50),
                    color = CarbonBlack.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, callout.color)
                ) {
                    Text(
                        text = callout.text,
                        style = MaterialTheme.typography.labelLarge,
                        color = callout.color,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 3. Countdown Lights Overlay
        if (snapshot.isCountingDown) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = CarbonBlack.copy(alpha = 0.86f),
                    border = BorderStroke(2.dp, RacingCrimson),
                    modifier = Modifier.size(110.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${snapshot.countdownNumber}",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 54.sp),
                            color = ChampionshipAmber
                        )
                    }
                }
            }
        }

        // 4. Lower Telemetry Cluster & Responsive Touch Controls
        if (!snapshot.isFinished && !isPaused) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Digital Speedometer + Nitro Gauge Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CarbonBlack.copy(alpha = 0.80f),
                        border = BorderStroke(1.dp, SubtleBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${snapshot.speedKmh}",
                                style = MaterialTheme.typography.displaySmall,
                                color = if (snapshot.isNitroActive) NitroCyan else TextPrimary
                            )
                            Column {
                                Text("KM/H", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("G${snapshot.gear}", style = MaterialTheme.typography.labelSmall, color = ChampionshipAmber)
                            }
                        }
                    }

                    // Nitro Bar
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CarbonBlack.copy(alpha = 0.80f),
                        border = BorderStroke(1.dp, NitroCyan.copy(alpha = 0.45f)),
                        modifier = Modifier.width(150.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("NITRO", style = MaterialTheme.typography.labelSmall, color = NitroCyan)
                                Text("${(snapshot.nitroNorm * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                            }
                            LinearProgressIndicator(
                                progress = { snapshot.nitroNorm },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = if (snapshot.isNitroActive) ChampionshipAmber else NitroCyan,
                                trackColor = PitSurface
                            )
                        }
                    }
                }

                // Multi-touch Ergonomic Steering & Pedals Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.settings.steeringMode == SteeringMode.TOUCH_BUTTONS) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            HoldControlPad(
                                label = "LEFT",
                                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                                active = leftPressed,
                                accent = TextPrimary,
                                modifier = Modifier
                                    .size(72.dp)
                                    .testTag("steer_left_pad"),
                                onPressChanged = { leftPressed = it }
                            )
                            HoldControlPad(
                                label = "RIGHT",
                                icon = Icons.AutoMirrored.Rounded.ArrowForward,
                                active = rightPressed,
                                accent = TextPrimary,
                                modifier = Modifier
                                    .size(72.dp)
                                    .testTag("steer_right_pad"),
                                onPressChanged = { rightPressed = it }
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CarbonBlack.copy(alpha = 0.72f)
                        ) {
                            Text(
                                text = if (state.settings.steeringMode == SteeringMode.TILT_SENSOR) "TILT PHONE TO STEER"
                                else "DRAG ROAD TO STEER",
                                style = MaterialTheme.typography.labelSmall,
                                color = NitroCyan,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HoldControlPad(
                            label = "BRAKE",
                            icon = null,
                            active = brakePressed,
                            accent = DangerRed,
                            modifier = Modifier
                                .size(68.dp)
                                .testTag("brake_pad"),
                            onPressChanged = { brakePressed = it }
                        )
                        HoldControlPad(
                            label = "NITRO",
                            icon = Icons.Rounded.Bolt,
                            active = nitroPressed,
                            accent = NitroCyan,
                            modifier = Modifier
                                .size(76.dp)
                                .testTag("nitro_pad"),
                            onPressChanged = { nitroPressed = it }
                        )
                    }
                }
            }
        }

        // 5. Pause Overlay
        if (isPaused && !snapshot.isFinished) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CarbonBlack.copy(alpha = 0.84f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.86f),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = PitSurface),
                    border = BorderStroke(1.dp, SubtleBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(stringResource(R.string.label_pause), style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                        Text("${state.selectedTrack.name} • ${state.selectedMode.title}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Button(
                            onClick = {
                                isPaused = false
                                simulation.setPaused(false)
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RacingCrimson)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.label_resume), style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = {
                                isPaused = false
                                raceRestartSeed++
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Rounded.Replay, contentDescription = null, tint = TextPrimary)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.label_restart), style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                        }
                        OutlinedButton(
                            onClick = { onExitToPit(activity) },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(stringResource(R.string.label_quit_pit), style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        }
                    }
                }
            }
        }

        // 6. Post-Race Podium & AdMob Rewarded 2x Credits Overlay
        if (snapshot.isFinished) {
            PostRaceResultsOverlay(
                state = state,
                snapshot = snapshot,
                earnedCredits = earnedCredits,
                adDoubled = adDoubled,
                onDoubleCredits = {
                    onDoubleCreditsAd(activity, earnedCredits, savedRecordId) {
                        adDoubled = true
                    }
                },
                onRaceAgain = { raceRestartSeed++ },
                onExitToPit = { onExitToPit(activity) }
            )
        }
    }
}

@Composable
private fun HoldControlPad(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    active: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onPressChanged: (Boolean) -> Unit
) {
    Surface(
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    when (event.type) {
                        PointerEventType.Press -> onPressChanged(true)
                        PointerEventType.Release -> onPressChanged(false)
                        PointerEventType.Exit -> onPressChanged(false)
                    }
                }
            }
        },
        shape = CircleShape,
        color = if (active) accent.copy(alpha = 0.35f) else CarbonBlack.copy(alpha = 0.72f),
        border = BorderStroke(if (active) 2.dp else 1.2.dp, accent.copy(alpha = if (active) 1f else 0.75f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(26.dp))
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = accent)
        }
    }
}

@Composable
private fun PostRaceResultsOverlay(
    state: MainUiState,
    snapshot: RaceSnapshot,
    earnedCredits: Int,
    adDoubled: Boolean,
    onDoubleCredits: () -> Unit,
    onRaceAgain: () -> Unit,
    onExitToPit: () -> Unit
) {
    val title = when (state.selectedMode) {
        RaceMode.CITY_SPRINT -> when (snapshot.position) {
            1 -> "1ST PLACE VICTORY!"
            2 -> "2ND PLACE PODIUM!"
            3 -> "3RD PLACE FINISH!"
            else -> "FINISHED P${snapshot.position}"
        }
        RaceMode.TIME_ATTACK -> when (snapshot.position) {
            1 -> "GOLD MEDAL RUN!"
            2 -> "SILVER MEDAL RUN!"
            else -> "BRONZE FINISH"
        }
        RaceMode.ENDLESS_HIGHWAY -> "HIGHWAY RUN COMPLETE"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonBlack.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .testTag("post_race_results_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PitSurfaceElevated),
            border = BorderStroke(1.5.dp, ChampionshipAmber.copy(alpha = 0.65f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Driver Portrait + Trophy
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = state.selectedDriver.portraitRes),
                        contentDescription = state.selectedDriver.name,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .border(2.dp, ChampionshipAmber, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Column {
                        Text(title, style = MaterialTheme.typography.headlineMedium, color = ChampionshipAmber)
                        Text(
                            text = "${state.selectedDriver.name} • ${state.selectedCar.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                // Telemetry Summary
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PitSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ResultStatRow("RACE TIME", formatRaceTime(snapshot.elapsedTimeMs))
                        ResultStatRow("TOP SPEED", "${snapshot.topSpeedKmh} KM/H")
                        ResultStatRow("NEAR MISSES", "${snapshot.nearMisses}")
                        ResultStatRow("DISTANCE", String.format("%.2f KM", snapshot.distanceKm))
                        ResultStatRow(
                            "CREDITS EARNED",
                            if (adDoubled) "+${earnedCredits * 2} CR (2X!)" else "+$earnedCredits CR",
                            highlightColor = TelemetryEmerald
                        )
                    }
                }

                // AdMob Rewarded Video 2x Payout CTA
                if (!adDoubled) {
                    Button(
                        onClick = onDoubleCredits,
                        enabled = state.isOnline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("double_credits_ad_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChampionshipAmber,
                            contentColor = CarbonBlack
                        )
                    ) {
                        Icon(Icons.Rounded.PlayCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (state.isOnline) "2X CREDITS (+$earnedCredits CR) • WATCH AD"
                            else "OFFLINE • 2X AD BONUS UNAVAILABLE",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onExitToPit,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("exit_to_pit_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("PIT HUB", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    }
                    Button(
                        onClick = onRaceAgain,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("race_again_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RacingCrimson)
                    ) {
                        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("RACE AGAIN", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultStatRow(label: String, value: String, highlightColor: Color = TextPrimary) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.labelLarge, color = highlightColor, fontWeight = FontWeight.Bold)
    }
}
