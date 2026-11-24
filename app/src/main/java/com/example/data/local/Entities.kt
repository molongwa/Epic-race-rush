package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val credits: Int = 1200,
    val selectedCarId: String = "apex_v8_gt",
    val selectedDriverId: String = "marcus",
    val totalRaces: Int = 0,
    val totalWins: Int = 0,
    val totalDistanceKm: Float = 0f,
    val bestEndlessScore: Int = 0,
    val racesSinceLastInterstitial: Int = 0
)

@Entity(tableName = "owned_cars")
data class OwnedCarEntity(
    @PrimaryKey val carId: String,
    val isUnlocked: Boolean,
    val engineLevel: Int = 1,
    val turboLevel: Int = 1,
    val handlingLevel: Int = 1,
    val nitroLevel: Int = 1,
    val paintHex: Long
)

@Entity(tableName = "race_records")
data class RaceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modeId: String,
    val trackId: String,
    val carId: String,
    val driverId: String,
    val position: Int,
    val elapsedTimeMs: Long,
    val topSpeedKmh: Int,
    val nearMisses: Int,
    val score: Int,
    val creditsEarned: Int,
    val timestamp: Long = System.currentTimeMillis()
)
