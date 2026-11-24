package com.example

import android.app.Application
import com.example.ads.AdMobManager
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsRepository
import com.example.data.repository.RacingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ApexRacingApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val racingRepository: RacingRepository by lazy { RacingRepository(database.racingDao()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val adMobManager: AdMobManager by lazy { AdMobManager(this) }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            racingRepository.ensureSeeded()
        }
        adMobManager.initialize()
    }
}
