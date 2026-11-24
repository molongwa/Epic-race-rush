package com.example.data.repository

import com.example.data.local.OwnedCarEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.RaceRecordEntity
import com.example.data.local.RacingDao
import com.example.model.RacingCatalog
import com.example.model.UpgradePart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RacingRepository(private val dao: RacingDao) {

    val profileFlow: Flow<PlayerProfileEntity> = dao.observeProfile().map {
        it ?: PlayerProfileEntity()
    }

    val ownedCarsFlow: Flow<List<OwnedCarEntity>> = dao.observeOwnedCars()
    val recentRecordsFlow: Flow<List<RaceRecordEntity>> = dao.observeRecentRecords()

    suspend fun ensureSeeded() {
        if (dao.getProfileOnce() == null) {
            dao.upsertProfile(PlayerProfileEntity())
        }
        val existing = dao.getOwnedCarsOnce().associateBy { it.carId }
        for (car in RacingCatalog.cars) {
            if (car.id !in existing) {
                dao.upsertOwnedCar(
                    OwnedCarEntity(
                        carId = car.id,
                        isUnlocked = car.priceCredits == 0,
                        paintHex = car.defaultPaintHex
                    )
                )
            }
        }
    }

    suspend fun selectCar(carId: String) {
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        val car = dao.getOwnedCar(carId)
        if (car?.isUnlocked == true) {
            dao.upsertProfile(profile.copy(selectedCarId = carId))
        }
    }

    suspend fun selectDriver(driverId: String) {
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        dao.upsertProfile(profile.copy(selectedDriverId = driverId))
    }

    suspend fun updateCarPaint(carId: String, paintHex: Long) {
        val car = dao.getOwnedCar(carId) ?: return
        dao.upsertOwnedCar(car.copy(paintHex = paintHex))
    }

    suspend fun unlockCar(carId: String): Boolean {
        val catalogCar = RacingCatalog.carById(carId)
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        val owned = dao.getOwnedCar(carId) ?: return false
        if (owned.isUnlocked) return true
        if (profile.credits < catalogCar.priceCredits) return false

        dao.upsertProfile(
            profile.copy(
                credits = profile.credits - catalogCar.priceCredits,
                selectedCarId = carId
            )
        )
        dao.upsertOwnedCar(owned.copy(isUnlocked = true))
        return true
    }

    fun upgradeCost(part: UpgradePart, currentLevel: Int): Int {
        return part.baseCost * currentLevel
    }

    suspend fun upgradeCarPart(carId: String, part: UpgradePart): Boolean {
        val profile = dao.getProfileOnce() ?: return false
        val owned = dao.getOwnedCar(carId) ?: return false
        if (!owned.isUnlocked) return false

        val currentLevel = when (part) {
            UpgradePart.ENGINE -> owned.engineLevel
            UpgradePart.TURBO -> owned.turboLevel
            UpgradePart.HANDLING -> owned.handlingLevel
            UpgradePart.NITRO -> owned.nitroLevel
        }
        if (currentLevel >= 5) return false

        val cost = upgradeCost(part, currentLevel)
        if (profile.credits < cost) return false

        val updatedCar = when (part) {
            UpgradePart.ENGINE -> owned.copy(engineLevel = currentLevel + 1)
            UpgradePart.TURBO -> owned.copy(turboLevel = currentLevel + 1)
            UpgradePart.HANDLING -> owned.copy(handlingLevel = currentLevel + 1)
            UpgradePart.NITRO -> owned.copy(nitroLevel = currentLevel + 1)
        }
        dao.upsertProfile(profile.copy(credits = profile.credits - cost))
        dao.upsertOwnedCar(updatedCar)
        return true
    }

    suspend fun recordCompletedRace(
        modeId: String,
        trackId: String,
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
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        val isWin = (modeId == "city_sprint" && position == 1) ||
            (modeId == "time_attack" && position == 1)
        val recordId = dao.insertRaceRecord(
            RaceRecordEntity(
                modeId = modeId,
                trackId = trackId,
                carId = carId,
                driverId = driverId,
                position = position,
                elapsedTimeMs = elapsedTimeMs,
                topSpeedKmh = topSpeedKmh,
                nearMisses = nearMisses,
                score = score,
                creditsEarned = creditsEarned
            )
        )
        dao.upsertProfile(
            profile.copy(
                credits = profile.credits + creditsEarned,
                totalRaces = profile.totalRaces + 1,
                totalWins = profile.totalWins + if (isWin) 1 else 0,
                totalDistanceKm = profile.totalDistanceKm + distanceKm,
                bestEndlessScore = maxOf(profile.bestEndlessScore, score),
                racesSinceLastInterstitial = profile.racesSinceLastInterstitial + 1
            )
        )
        return recordId
    }

    suspend fun resetInterstitialCounter() {
        val profile = dao.getProfileOnce() ?: return
        dao.upsertProfile(profile.copy(racesSinceLastInterstitial = 0))
    }

    suspend fun grantAdCredits(amount: Int, recordId: Long? = null) {
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        dao.upsertProfile(profile.copy(credits = profile.credits + amount))
        if (recordId != null) {
            dao.addBonusCreditsToRecord(recordId, amount)
        }
    }
}
