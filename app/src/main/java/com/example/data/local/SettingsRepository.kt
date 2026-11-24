package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.SteeringMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "apex_settings")

data class GameSettings(
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val steeringMode: SteeringMode = SteeringMode.TOUCH_BUTTONS
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val STEERING_MODE = stringPreferencesKey("steering_mode")
    }

    val settingsFlow: Flow<GameSettings> = context.dataStore.data.map { prefs ->
        val modeStr = prefs[Keys.STEERING_MODE] ?: SteeringMode.TOUCH_BUTTONS.name
        val mode = runCatching { SteeringMode.valueOf(modeStr) }
            .getOrDefault(SteeringMode.TOUCH_BUTTONS)
        GameSettings(
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            steeringMode = mode
        )
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setSteeringMode(mode: SteeringMode) {
        context.dataStore.edit { it[Keys.STEERING_MODE] = mode.name }
    }
}
