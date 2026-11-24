package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ads.AdMobBannerBar
import com.example.ui.GameViewModel
import com.example.ui.screens.GarageScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RaceScreen
import com.example.ui.screens.RecordsScreen
import com.example.ui.screens.SettingsSheet
import com.example.ui.theme.ApexCityRushTheme
import com.example.ui.theme.AsphaltNavy
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.PitSurfaceElevated
import com.example.ui.theme.RacingCrimson
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

const val ROUTE_PIT_HUB = "pit_hub"
const val ROUTE_GARAGE = "garage"
const val ROUTE_RECORDS = "records"
const val ROUTE_RACE = "race"

private data class BottomTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ApexRacingApp
        setContent {
            ApexCityRushTheme {
                ApexCityRushAppRoot(app = app)
            }
        }
    }
}

@Composable
fun ApexCityRushAppRoot(app: ApexRacingApp) {
    val viewModel: GameViewModel = viewModel(factory = GameViewModel.factory(app))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_PIT_HUB
    val isRacing = currentRoute == ROUTE_RACE

    var showSettings by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.bannerMessage) {
        val msg = state.bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.dismissBannerMessage()
    }

    val tabs = listOf(
        BottomTab(ROUTE_PIT_HUB, R.string.nav_pit_hub, Icons.Rounded.Home, "nav_pit_hub"),
        BottomTab(ROUTE_GARAGE, R.string.nav_garage, Icons.Rounded.DirectionsCar, "nav_garage"),
        BottomTab(ROUTE_RECORDS, R.string.nav_records, Icons.Rounded.EmojiEvents, "nav_records")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CarbonBlack,
        contentWindowInsets = if (isRacing) WindowInsets(0, 0, 0, 0) else WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (!isRacing) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    AdMobBannerBar(adMobManager = app.adMobManager)
                    NavigationBar(
                        containerColor = AsphaltNavy,
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TextPrimary,
                                    selectedTextColor = TextPrimary,
                                    indicatorColor = RacingCrimson,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_PIT_HUB,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(ROUTE_PIT_HUB) {
                HomeScreen(
                    state = state,
                    onSelectMode = viewModel::selectMode,
                    onSelectTrack = viewModel::selectTrack,
                    onSelectCar = viewModel::selectCar,
                    onUnlockCar = viewModel::unlockCar,
                    onStartRace = { navController.navigate(ROUTE_RACE) },
                    onOpenGarage = {
                        navController.navigate(ROUTE_GARAGE) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenSettings = { showSettings = true }
                )
            }

            composable(ROUTE_GARAGE) {
                GarageScreen(
                    state = state,
                    upgradeCostProvider = viewModel::upgradeCost,
                    onSelectCar = viewModel::selectCar,
                    onUnlockCar = viewModel::unlockCar,
                    onUpgradePart = viewModel::upgradePart,
                    onUpdatePaint = viewModel::updatePaint,
                    onSelectDriver = viewModel::selectDriver,
                    onWatchFreeCreditsAd = viewModel::watchFreeCreditsAd
                )
            }

            composable(ROUTE_RECORDS) {
                RecordsScreen(state = state)
            }

            composable(ROUTE_RACE) {
                RaceScreen(
                    state = state,
                    onSaveRaceResult = { mode, pos, elapsedMs, distKm, topSpeed, nearMisses, score, credits ->
                        viewModel.saveRaceResult(
                            mode = mode,
                            track = state.selectedTrack,
                            carId = state.selectedCar.id,
                            driverId = state.selectedDriver.id,
                            position = pos,
                            elapsedTimeMs = elapsedMs,
                            distanceKm = distKm,
                            topSpeedKmh = topSpeed,
                            nearMisses = nearMisses,
                            score = score,
                            creditsEarned = credits
                        )
                    },
                    onDoubleCreditsAd = viewModel::watchDoubleCreditsAd,
                    onExitToPit = { activity ->
                        viewModel.maybeShowPostRaceInterstitial(activity) {
                            navController.popBackStack()
                        }
                    }
                )
            }
        }
    }

    if (showSettings) {
        SettingsSheet(
            state = state,
            onDismiss = { showSettings = false },
            onToggleSound = viewModel::setSoundEnabled,
            onToggleHaptics = viewModel::setHapticsEnabled,
            onSelectSteeringMode = viewModel::setSteeringMode
        )
    }
}
