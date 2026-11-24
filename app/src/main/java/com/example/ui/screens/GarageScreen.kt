package com.example.ui.screens

import android.app.Activity
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Upgrade
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.OwnedCarEntity
import com.example.model.CarModel
import com.example.model.DriverProfile
import com.example.model.RacingCatalog
import com.example.model.UpgradePart
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
fun GarageScreen(
    state: MainUiState,
    upgradeCostProvider: (UpgradePart, Int) -> Int,
    onSelectCar: (String) -> Unit,
    onUnlockCar: (String) -> Unit,
    onUpgradePart: (String, UpgradePart) -> Unit,
    onUpdatePaint: (String, Long) -> Unit,
    onSelectDriver: (String) -> Unit,
    onWatchFreeCreditsAd: (Activity?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var inspectedCarId by remember(state.profile.selectedCarId) {
        mutableStateOf(state.profile.selectedCarId)
    }
    val context = LocalContext.current
    val activity = context as? Activity

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
        ) {
            // Garage Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PIT GARAGE",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = PitSurfaceElevated,
                    border = BorderStroke(1.dp, ChampionshipAmber.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${state.profile.credits} CR",
                        style = MaterialTheme.typography.labelLarge,
                        color = ChampionshipAmber,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = AsphaltNavy,
                contentColor = TextPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = RacingCrimson
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("SUPERCARS", style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.testTag("tab_supercars")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("REAL DRIVERS", style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.testTag("tab_drivers")
                )
            }

            if (selectedTab == 0) {
                val inspectedCar = RacingCatalog.carById(inspectedCarId)
                val owned = state.ownedCars[inspectedCar.id] ?: OwnedCarEntity(
                    carId = inspectedCar.id,
                    isUnlocked = inspectedCar.priceCredits == 0,
                    paintHex = inspectedCar.defaultPaintHex
                )
                CarsGarageTab(
                    state = state,
                    inspectedCar = inspectedCar,
                    owned = owned,
                    upgradeCostProvider = upgradeCostProvider,
                    onInspectCar = { inspectedCarId = it },
                    onSelectCar = onSelectCar,
                    onUnlockCar = onUnlockCar,
                    onUpgradePart = onUpgradePart,
                    onUpdatePaint = onUpdatePaint,
                    onWatchFreeCreditsAd = { onWatchFreeCreditsAd(activity) }
                )
            } else {
                DriversGarageTab(
                    selectedDriverId = state.profile.selectedDriverId,
                    onSelectDriver = onSelectDriver
                )
            }
        }
    }
}

@Composable
private fun CarsGarageTab(
    state: MainUiState,
    inspectedCar: CarModel,
    owned: OwnedCarEntity,
    upgradeCostProvider: (UpgradePart, Int) -> Int,
    onInspectCar: (String) -> Unit,
    onSelectCar: (String) -> Unit,
    onUnlockCar: (String) -> Unit,
    onUpgradePart: (String, UpgradePart) -> Unit,
    onUpdatePaint: (String, Long) -> Unit,
    onWatchFreeCreditsAd: () -> Unit
) {
    val isEquipped = state.profile.selectedCarId == inspectedCar.id

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("garage_supercar_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "fleet_header") {
            Text(
                text = "SUPERCAR FLEET (${RacingCatalog.cars.size})",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
        }

        // Vertical Supercar Selector List
        items(RacingCatalog.cars, key = { it.id }) { car ->
            val carOwned = state.ownedCars[car.id]?.isUnlocked == true
            val selected = car.id == inspectedCar.id
            val equipped = state.profile.selectedCarId == car.id
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onInspectCar(car.id) }
                    .testTag("garage_car_${car.id}"),
                shape = RoundedCornerShape(14.dp),
                color = if (selected) PitSurfaceElevated else PitSurface,
                border = BorderStroke(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) RacingCrimson else SubtleBorder
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (carOwned) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = if (carOwned) TelemetryEmerald else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = car.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "${car.baseTopSpeedKmh} KM/H • ${car.engineSpec}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                    Text(
                        text = when {
                            equipped -> "EQUIPPED"
                            carOwned -> car.classLabel
                            else -> "${car.priceCredits} CR"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            equipped -> TelemetryEmerald
                            carOwned -> NitroCyan
                            else -> ChampionshipAmber
                        }
                    )
                }
            }
        }

        item(key = "showcase_card") {
        // Photorealistic Showroom Showcase Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PitSurface),
            border = BorderStroke(1.dp, SubtleBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    Image(
                        painter = painterResource(id = inspectedCar.showcaseImageRes),
                        contentDescription = inspectedCar.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, CarbonBlack.copy(alpha = 0.88f))
                                )
                            )
                    )
                    // Custom paint accent bar
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(owned.paintHex))
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = inspectedCar.classLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = inspectedCar.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = inspectedCar.engineSpec,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                // Performance Spec Telemetry Bars
                val effectiveTopSpeed = inspectedCar.baseTopSpeedKmh +
                    (owned.engineLevel - 1) * 14 + (owned.turboLevel - 1) * 6
                val effectiveAccel = (inspectedCar.baseZeroToHundredSec -
                    (owned.turboLevel - 1) * 0.14f).coerceAtLeast(1.8f)
                val effectiveHandling = (inspectedCar.baseHandling + (owned.handlingLevel - 1) * 0.04f)
                    .coerceAtMost(1f)
                val effectiveNitro = (inspectedCar.baseNitroCapacity + (owned.nitroLevel - 1) * 0.05f)
                    .coerceAtMost(1f)

                SpecBarRow("TOP SPEED", "$effectiveTopSpeed KM/H", effectiveTopSpeed / 420f, RacingCrimson)
                SpecBarRow("0-100 KM/H", String.format("%.1fs", effectiveAccel), (4.0f - effectiveAccel) / 2.5f, ChampionshipAmber)
                SpecBarRow("HANDLING", "${(effectiveHandling * 100).toInt()}%", effectiveHandling, TelemetryEmerald)
                SpecBarRow("NITRO CAPACITY", "${(effectiveNitro * 100).toInt()}%", effectiveNitro, NitroCyan)

                // Paint Swatches
                if (owned.isUnlocked) {
                    Text(
                        text = "FACTORY METALLIC PAINT",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RacingCatalog.paintSwatches.forEach { swatch ->
                            val activePaint = owned.paintHex == swatch.argbLong
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(swatch.color)
                                    .border(
                                        width = if (activePaint) 3.dp else 1.dp,
                                        color = if (activePaint) Color.White else SubtleBorder,
                                        shape = CircleShape
                                    )
                                    .clickable { onUpdatePaint(inspectedCar.id, swatch.argbLong) }
                            )
                        }
                    }
                }

                // Equip or Unlock Action
                if (!owned.isUnlocked) {
                    Button(
                        onClick = { onUnlockCar(inspectedCar.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("unlock_car_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChampionshipAmber, contentColor = CarbonBlack)
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("UNLOCK FOR ${inspectedCar.priceCredits} CR", style = MaterialTheme.typography.labelLarge)
                    }
                } else if (!isEquipped) {
                    Button(
                        onClick = { onSelectCar(inspectedCar.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("equip_car_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RacingCrimson)
                    ) {
                        Text("SELECT FOR RACE", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = TelemetryEmerald.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, TelemetryEmerald.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = TelemetryEmerald, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("EQUIPPED IN PIT LANE", style = MaterialTheme.typography.labelLarge, color = TelemetryEmerald)
                        }
                    }
                }
            }
        }
        }

        // Performance Tuning Upgrades
        if (owned.isUnlocked) {
            item(key = "tuning_header") {
                Text(
                    text = "PERFORMANCE TUNING",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
            }
            items(UpgradePart.entries, key = { it.name }) { part ->
                val lvl = when (part) {
                    UpgradePart.ENGINE -> owned.engineLevel
                    UpgradePart.TURBO -> owned.turboLevel
                    UpgradePart.HANDLING -> owned.handlingLevel
                    UpgradePart.NITRO -> owned.nitroLevel
                }
                val cost = upgradeCostProvider(part, lvl)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PitSurface,
                    border = BorderStroke(1.dp, SubtleBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(part.label, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("${part.statLabel} • Level $lvl / 5", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        if (lvl >= 5) {
                            Text("MAXED", style = MaterialTheme.typography.labelLarge, color = TelemetryEmerald)
                        } else {
                            OutlinedButton(
                                onClick = { onUpgradePart(inspectedCar.id, part) },
                                border = BorderStroke(1.dp, NitroCyan),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("upgrade_${part.name.lowercase()}")
                            ) {
                                Icon(Icons.Rounded.Upgrade, contentDescription = null, tint = NitroCyan, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("$cost CR", style = MaterialTheme.typography.labelLarge, color = NitroCyan)
                            }
                        }
                    }
                }
            }
        }

        // AdMob Rewarded Video Card for Free Garage Credits
        item(key = "rewarded_ad_card") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PitSurfaceElevated),
                border = BorderStroke(1.dp, ChampionshipAmber.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayCircle,
                        contentDescription = "Rewarded Ad",
                        tint = ChampionshipAmber,
                        modifier = Modifier.size(36.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SPONSORSHIP BONUS",
                            style = MaterialTheme.typography.labelLarge,
                            color = ChampionshipAmber
                        )
                        Text(
                            text = if (state.isOnline) "Watch a short video ad to earn +500 instant Credits."
                            else "Connect to internet when ready to claim +500 Ad Credits.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Button(
                        onClick = onWatchFreeCreditsAd,
                        enabled = state.isOnline,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChampionshipAmber, contentColor = CarbonBlack),
                        modifier = Modifier.testTag("garage_rewarded_ad_button")
                    ) {
                        Text(
                            text = stringResource(R.string.ad_free_credits),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SpecBarRow(label: String, valueText: String, progress: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(valueText, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0.05f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = color,
            trackColor = CarbonBlack
        )
    }
}

@Composable
private fun DriversGarageTab(
    selectedDriverId: String,
    onSelectDriver: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "drivers_header") {
            Text(
                text = "CHOOSE YOUR HUMAN DRIVER",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
        }
        items(RacingCatalog.drivers, key = { it.id }) { driver ->
            DriverProfileCard(
                driver = driver,
                isSelected = driver.id == selectedDriverId,
                onSelect = { onSelectDriver(driver.id) }
            )
        }
    }
}

@Composable
private fun DriverProfileCard(
    driver: DriverProfile,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("driver_card_${driver.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) PitSurfaceElevated else PitSurface),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) NitroCyan else SubtleBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Image(
                    painter = painterResource(id = driver.portraitRes),
                    contentDescription = driver.name,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, if (isSelected) NitroCyan else SubtleBorder, RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = RacingCrimson.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = driver.callsign,
                                style = MaterialTheme.typography.labelSmall,
                                color = RacingCrimson,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = driver.cityOrigin,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = driver.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = NitroCyan, modifier = Modifier.size(16.dp))
                        Text(driver.perkTitle, style = MaterialTheme.typography.labelLarge, color = NitroCyan)
                    }
                }
            }
            Text(
                text = driver.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = "Perk: ${driver.perkDescription}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) TelemetryEmerald else RacingCrimson,
                    contentColor = if (isSelected) CarbonBlack else TextPrimary
                )
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.Person,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isSelected) "ACTIVE DRIVER" else "SELECT ${driver.name.uppercase()}",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
