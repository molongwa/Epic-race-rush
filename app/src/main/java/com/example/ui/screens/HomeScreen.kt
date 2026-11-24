package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OfflineBolt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.OwnedCarEntity
import com.example.model.CarModel
import com.example.model.CityTrack
import com.example.model.RaceMode
import com.example.model.RacingCatalog
import com.example.ui.MainUiState
import com.example.ui.theme.AsphaltNavy
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.ChampionshipAmber
import com.example.ui.theme.NitroCyan
import com.example.ui.theme.PitSurface
import com.example.ui.theme.PitSurfaceElevated
import com.example.ui.theme.RacingCrimson
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TelemetryEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    state: MainUiState,
    onSelectMode: (RaceMode) -> Unit,
    onSelectTrack: (CityTrack) -> Unit,
    onSelectCar: (String) -> Unit,
    onUnlockCar: (String) -> Unit,
    onStartRace: () -> Unit,
    onOpenGarage: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        // Atmospheric City Highway Backdrop
        Image(
            painter = painterResource(id = R.drawable.img_city_hero_skyline),
            contentDescription = "City skyline highway backdrop",
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CarbonBlack.copy(alpha = 0.55f),
                            CarbonBlack.copy(alpha = 0.80f),
                            CarbonBlack
                        )
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .testTag("home_supercar_lazy_column"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Top Header Telemetry Row
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "APEX CITY RUSH",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (state.isOnline) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                                contentDescription = null,
                                tint = if (state.isOnline) TelemetryEmerald else NitroCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (state.isOnline) "ONLINE • ADMOB READY" else stringResource(R.string.offline_ready_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.isOnline) TelemetryEmerald else NitroCyan
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = PitSurfaceElevated,
                            border = BorderStroke(1.dp, ChampionshipAmber.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.EmojiEvents,
                                    contentDescription = "Credits",
                                    tint = ChampionshipAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${state.profile.credits} CR",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ChampionshipAmber
                                )
                            }
                        }

                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(PitSurfaceElevated.copy(alpha = 0.85f))
                                .testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Settings",
                                tint = TextPrimary
                            )
                        }
                    }
                }
            }

            // 2. Active Driver + Selected Vehicle Pit Banner
            item(key = "active_setup") {
                ActiveSetupCard(
                    state = state,
                    onOpenGarage = onOpenGarage
                )
            }

            // 3. Vertical Supercar Selector List Header
            item(key = "supercar_list_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SELECT SUPERCAR (${RacingCatalog.cars.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    Text(
                        text = "TUNE IN GARAGE >",
                        style = MaterialTheme.typography.labelSmall,
                        color = RacingCrimson,
                        modifier = Modifier
                            .clickable(onClick = onOpenGarage)
                            .padding(vertical = 4.dp)
                    )
                }
            }

            // 4. Vertical List of Supercars inside LazyColumn
            items(RacingCatalog.cars, key = { it.id }) { car ->
                val owned = state.ownedCars[car.id] ?: OwnedCarEntity(
                    carId = car.id,
                    isUnlocked = car.priceCredits == 0,
                    paintHex = car.defaultPaintHex
                )
                val isSelected = state.profile.selectedCarId == car.id
                PreRaceSupercarItemCard(
                    car = car,
                    owned = owned,
                    isSelected = isSelected,
                    canAfford = state.profile.credits >= car.priceCredits,
                    onSelect = { onSelectCar(car.id) },
                    onUnlock = { onUnlockCar(car.id) }
                )
            }

            // 5. City Circuit Selection
            item(key = "city_circuits") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CITY CIRCUITS",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(end = 8.dp)
                    ) {
                        items(RacingCatalog.tracks, key = { it.id }) { track ->
                            val isSelected = state.selectedTrack.id == track.id
                            TrackChipCard(
                                track = track,
                                isSelected = isSelected,
                                onClick = { onSelectTrack(track) }
                            )
                        }
                    }
                }
            }

            // 6. Offline Play Modes Header
            item(key = "modes_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OFFLINE PLAY MODES",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    Text(
                        text = "100% Playable Without Wi-Fi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NitroCyan
                    )
                }
            }

            // 7. Offline Play Mode Cards
            items(RaceMode.entries, key = { it.id }) { mode ->
                val selected = state.selectedMode == mode
                ModeSelectorCard(
                    mode = mode,
                    selected = selected,
                    onClick = { onSelectMode(mode) }
                )
            }

            // 8. Primary Launch Race CTA
            item(key = "race_now_cta") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onStartRace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("race_now_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RacingCrimson,
                            contentColor = TextPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${stringResource(R.string.cta_race_now)} • ${state.selectedCar.name.uppercase()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun PreRaceSupercarItemCard(
    car: CarModel,
    owned: OwnedCarEntity,
    isSelected: Boolean,
    canAfford: Boolean,
    onSelect: () -> Unit,
    onUnlock: () -> Unit
) {
    val tunedTopSpeed = car.baseTopSpeedKmh + (owned.engineLevel - 1) * 14 + (owned.turboLevel - 1) * 6
    val effectiveHandling = (car.baseHandling + (owned.handlingLevel - 1) * 0.04f).coerceAtMost(1f)
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> RacingCrimson
            owned.isUnlocked -> SubtleBorder
            else -> SubtleBorder.copy(alpha = 0.65f)
        },
        label = "carCardBorder"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (owned.isUnlocked) onSelect() else onUnlock()
            }
            .testTag("home_car_item_${car.id}"),
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) PitSurfaceElevated else PitSurface,
        border = BorderStroke(if (isSelected) 1.8.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Vehicle Thumbnail with Paint Swatch Badge
            Box(
                modifier = Modifier
                    .size(width = 104.dp, height = 76.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Image(
                    painter = painterResource(id = car.showcaseImageRes),
                    contentDescription = car.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            CarbonBlack.copy(alpha = if (owned.isUnlocked) 0.18f else 0.55f)
                        )
                )
                // Paint color indicator strip
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(width = 24.dp, height = 8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(owned.paintHex))
                        .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(50))
                )
                if (!owned.isUnlocked) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Locked vehicle",
                        tint = ChampionshipAmber,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp)
                    )
                }
            }

            // Specs & Status Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = car.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = RacingCrimson
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "SELECTED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    } else if (owned.isUnlocked) {
                        Text(
                            text = car.classLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = NitroCyan
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (canAfford) ChampionshipAmber else PitSurfaceElevated,
                            border = BorderStroke(1.dp, ChampionshipAmber.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = "${car.priceCredits} CR",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (canAfford) CarbonBlack else ChampionshipAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Text(
                    text = car.engineSpec,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Compact Speed & Handling Bars
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "$tunedTopSpeed KM/H",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
                        )
                        LinearProgressIndicator(
                            progress = { (tunedTopSpeed / 420f).coerceIn(0.1f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(50)),
                            color = RacingCrimson,
                            trackColor = CarbonBlack
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "GRIP ${(effectiveHandling * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = TelemetryEmerald
                        )
                        LinearProgressIndicator(
                            progress = { effectiveHandling },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(50)),
                            color = TelemetryEmerald,
                            trackColor = CarbonBlack
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveSetupCard(
    state: MainUiState,
    onOpenGarage: () -> Unit
) {
    val car = state.selectedCar
    val owned = state.selectedOwnedCar
    val driver = state.selectedDriver
    val tunedTopSpeed = car.baseTopSpeedKmh + (owned.engineLevel - 1) * 14 + (owned.turboLevel - 1) * 6

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenGarage)
            .testTag("active_setup_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PitSurface.copy(alpha = 0.94f)),
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Image(
                    painter = painterResource(id = car.showcaseImageRes),
                    contentDescription = car.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    CarbonBlack.copy(alpha = 0.85f),
                                    Color.Transparent,
                                    CarbonBlack.copy(alpha = 0.7f)
                                )
                            )
                        )
                )
                // Car name & paint badge
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Surface(
                        color = Color(owned.paintHex),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = car.classLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = car.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$tunedTopSpeed KM/H • ${car.engineSpec}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Real Human Driver Portrait Badge
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(CarbonBlack.copy(alpha = 0.82f))
                        .border(1.dp, NitroCyan.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = driver.portraitRes),
                        contentDescription = driver.name,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Column {
                        Text(
                            text = driver.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = driver.perkTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = NitroCyan
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatBadge(icon = Icons.Rounded.Speed, text = "LVL ${owned.engineLevel}/5 ENG")
                    StatBadge(icon = Icons.Rounded.Tune, text = "LVL ${owned.turboLevel}/5 TRB")
                }
                Text(
                    text = "GARAGE & DRIVERS >",
                    style = MaterialTheme.typography.labelLarge,
                    color = RacingCrimson
                )
            }
        }
    }
}

@Composable
private fun StatBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun TrackChipCard(
    track: CityTrack,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(track.buildingAccentColor) else SubtleBorder,
        label = "trackBorder"
    )
    Surface(
        modifier = Modifier
            .width(215.dp)
            .clickable(onClick = onClick)
            .testTag("track_card_${track.id}"),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) PitSurfaceElevated else AsphaltNavy,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = track.district.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(track.buildingAccentColor)
                )
                Text(
                    text = "${(track.lapLengthMeters / 1000f)} KM",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Text(
                text = track.name,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ModeSelectorCard(
    mode: RaceMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (mode) {
        RaceMode.CITY_SPRINT -> Icons.Rounded.Flag
        RaceMode.TIME_ATTACK -> Icons.Rounded.Timer
        RaceMode.ENDLESS_HIGHWAY -> Icons.Rounded.OfflineBolt
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("mode_card_${mode.id}"),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) PitSurfaceElevated else PitSurface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) RacingCrimson else SubtleBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected) RacingCrimson.copy(alpha = 0.22f)
                        else CarbonBlack.copy(alpha = 0.6f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = mode.title,
                    tint = if (selected) RacingCrimson else TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = mode.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CarbonBlack.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = mode.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) ChampionshipAmber else TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = mode.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Rounded.DirectionsCar,
                contentDescription = null,
                tint = if (selected) RacingCrimson else Color.Transparent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
