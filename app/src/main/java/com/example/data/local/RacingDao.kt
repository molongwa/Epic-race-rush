package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RacingDao {

    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun observeProfile(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getProfileOnce(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: PlayerProfileEntity)

    @Query("SELECT * FROM owned_cars")
    fun observeOwnedCars(): Flow<List<OwnedCarEntity>>

    @Query("SELECT * FROM owned_cars")
    suspend fun getOwnedCarsOnce(): List<OwnedCarEntity>

    @Query("SELECT * FROM owned_cars WHERE carId = :carId")
    suspend fun getOwnedCar(carId: String): OwnedCarEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOwnedCar(car: OwnedCarEntity)

    @Query("SELECT * FROM race_records ORDER BY timestamp DESC LIMIT 50")
    fun observeRecentRecords(): Flow<List<RaceRecordEntity>>

    @Query("SELECT MIN(elapsedTimeMs) FROM race_records WHERE trackId = :trackId AND modeId = :modeId")
    fun observeBestTimeForTrack(trackId: String, modeId: String): Flow<Long?>

    @Insert
    suspend fun insertRaceRecord(record: RaceRecordEntity): Long

    @Query("UPDATE race_records SET creditsEarned = creditsEarned + :bonusCredits WHERE id = :recordId")
    suspend fun addBonusCreditsToRecord(recordId: Long, bonusCredits: Int)

    @Query("DELETE FROM race_records")
    suspend fun clearRecords()
}
