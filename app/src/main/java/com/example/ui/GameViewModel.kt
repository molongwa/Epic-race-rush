package com.example.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ApexRacingApp
import com.example.ads.AdMobManager
import com.example.data.local.GameSettings
import com.example.data.local.OwnedCarEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.RaceRecordEntity
import com.example.data.local.SettingsRepository
import com.example.data.repository.RacingRepository
import com.example.model.CarModel
import com.example.model.CityTrack
import com.example.model.DriverProfile
import com.example.model.RaceMode
import com.example.model.RacingCatalog
import com.example.model.SteeringMode
import com.example.model.UpgradePart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val profile: PlayerProfileEntity = PlayerProfileEntity(),
    val ownedCars: Map<String, OwnedCarEntity> = emptyMap(),
    val selectedMode: RaceMode = RaceMode.CITY_SPRINT,
    val selectedTrack: CityTrack = RacingCatalog.tracks.first(),
    val recentRecords: List<RaceRecordEntity> = emptyList(),
    val settings: GameSettings = GameSettings(),
    val isOnline: Boolean = false,
    val isRewardedAdReady: Boolean = false,
    val isUsingTestAdIds: Boolean = true,
    val bannerMessage: String? = null
) {
    val selectedCar: CarModel
        get() = RacingCatalog.carById(profile.selectedCarId)

    val selectedOwnedCar: OwnedCarEntity
        get() = ownedCars[profile.selectedCarId] ?: OwnedCarEntity(
            carId = selectedCar.id,
            isUnlocked = true,
            paintHex = selectedCar.defaultPaintHex
        )

    val selectedDriver: DriverProfile
        get() = RacingCatalog.driverById(profile.selectedDriverId)
}

class GameViewModel(
    private val repository: RacingRepository,
    private val settingsRepository: SettingsRepository,
    private val adMobManager: AdMobManager
) : ViewModel() {

    private val selectedModeFlow = MutableStateFlow(RaceMode.CITY_SPRINT)
    private val selectedTrackFlow = MutableStateFlow(RacingCatalog.tracks.first())
    private val bannerMessageFlow = MutableStateFlow<String?>(null)

    private val coreDataFlow = combine(
        repository.profileFlow,
        repository.ownedCarsFlow,
        repository.recentRecordsFlow,
        settingsRepository.settingsFlow
    ) { profile, ownedList, records, settings ->
        Quad(profile, ownedList.associateBy { it.carId }, records, settings)
    }

    private val selectionFlow = combine(
        selectedModeFlow,
        selectedTrackFlow,
        adMobManager.isOnline,
        adMobManager.isRewardedReady,
        bannerMessageFlow
    ) { mode, track, online, rewardedReady, msg ->
        SelectionState(mode, track, online, rewardedReady, msg)
    }

    val uiState: StateFlow<MainUiState> = combine(
        coreDataFlow,
        selectionFlow
    ) { core, sel ->
        MainUiState(
            profile = core.profile,
            ownedCars = core.ownedCars,
            selectedMode = sel.mode,
            selectedTrack = sel.track,
            recentRecords = core.records,
            settings = core.settings,
            isOnline = sel.online,
            isRewardedAdReady = sel.rewardedReady,
            isUsingTestAdIds = adMobManager.isUsingTestIds,
            bannerMessage = sel.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    fun selectMode(mode: RaceMode) {
        selectedModeFlow.value = mode
    }

    fun selectTrack(track: CityTrack) {
        selectedTrackFlow.value = track
    }

    fun selectCar(carId: String) {
        viewModelScope.launch { repository.selectCar(carId) }
    }

    fun selectDriver(driverId: String) {
        viewModelScope.launch { repository.selectDriver(driverId) }
    }

    fun updatePaint(carId: String, paintHex: Long) {
        viewModelScope.launch { repository.updateCarPaint(carId, paintHex) }
    }

    fun unlockCar(carId: String) {
        viewModelScope.launch {
            val success = repository.unlockCar(carId)
            bannerMessageFlow.value = if (success) {
                "${RacingCatalog.carById(carId).name} unlocked & equipped!"
            } else {
                "Not enough credits! Race or watch a Rewarded Ad for +500 CR."
            }
        }
    }

    fun upgradePart(carId: String, part: UpgradePart) {
        viewModelScope.launch {
            val success = repository.upgradeCarPart(carId, part)
            if (!success) {
                bannerMessageFlow.value = "Need more credits or part is at Max Level."
            }
        }
    }

    fun upgradeCost(part: UpgradePart, currentLevel: Int): Int =
        repository.upgradeCost(part, currentLevel)

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSoundEnabled(enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHapticsEnabled(enabled) }
    }

    fun setSteeringMode(mode: SteeringMode) {
        viewModelScope.launch { settingsRepository.setSteeringMode(mode) }
    }

    suspend fun saveRaceResult(
        mode: RaceMode,
        track: CityTrack,
        carId: String,
        driverId: String,
        position: Int,
        elapsedTimeMs: Long,
        distanceKm: Float,
        topSpeedKmh: Int,
        nearMisses: Int,
        score: Int,
        creditsEarned: Int
    ): Long {
        return repository.recordCompletedRace(
            modeId = mode.id,
            trackId = track.id,
            carId = carId,
            driverId = driverId,
            position = position,
            elapsedTimeMs = elapsedTimeMs,
            distanceKm = distanceKm,
            topSpeedKmh = topSpeedKmh,
            nearMisses = nearMisses,
            score = score,
            creditsEarned = creditsEarned
        )
    }

    fun maybeShowPostRaceInterstitial(activity: Activity?, onContinue: () -> Unit) {
        val count = uiState.value.profile.racesSinceLastInterstitial
        if (count >= 2) {
            viewModelScope.launch { repository.resetInterstitialCounter() }
            adMobManager.showInterstitialIfReady(activity, onContinue)
        } else {
            onContinue()
        }
    }

    fun watchDoubleCreditsAd(
        activity: Activity?,
        bonusAmount: Int,
        recordId: Long?,
        onDoubled: () -> Unit
    ) {
        adMobManager.showRewardedAd(
            activity = activity,
            onRewardEarned = {
                viewModelScope.launch {
                    repository.grantAdCredits(bonusAmount, recordId)
                    bannerMessageFlow.value = "+$bonusAmount Bonus Credits added!"
                    onDoubled()
                }
            },
            onUnavailable = {
                bannerMessageFlow.value = if (uiState.value.isOnline) {
                    "Rewarded ad is still loading. Try again in a moment!"
                } else {
                    "You are offline. Connect to internet to earn Rewarded Ad bonuses."
                }
            }
        )
    }

    fun watchFreeCreditsAd(activity: Activity?) {
        adMobManager.showRewardedAd(
            activity = activity,
            onRewardEarned = {
                viewModelScope.launch {
                    repository.grantAdCredits(500)
                    bannerMessageFlow.value = "+500 Credits earned via Google AdMob!"
                }
            },
            onUnavailable = {
                bannerMessageFlow.value = if (uiState.value.isOnline) {
                    "Ad still preparing — check back in a few seconds."
                } else {
                    "Offline Mode active — connect to watch Rewarded Ads."
                }
            }
        )
    }

    fun dismissBannerMessage() {
        bannerMessageFlow.value = null
    }

    private data class Quad(
        val profile: PlayerProfileEntity,
        val ownedCars: Map<String, OwnedCarEntity>,
        val records: List<RaceRecordEntity>,
        val settings: GameSettings
    )

    private data class SelectionState(
        val mode: RaceMode,
        val track: CityTrack,
        val online: Boolean,
        val rewardedReady: Boolean,
        val message: String?
    )

    companion object {
        fun factory(app: ApexRacingApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GameViewModel(
                        repository = app.racingRepository,
                        settingsRepository = app.settingsRepository,
                        adMobManager = app.adMobManager
                    ) as T
                }
            }
    }
}
