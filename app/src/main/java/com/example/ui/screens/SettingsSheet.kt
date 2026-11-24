package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.SteeringMode
import com.example.ui.MainUiState
import com.example.ui.theme.AsphaltNavy
import com.example.ui.theme.ChampionshipAmber
import com.example.ui.theme.NitroCyan
import com.example.ui.theme.PitSurface
import com.example.ui.theme.RacingCrimson
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TelemetryEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    state: MainUiState,
    onDismiss: () -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onSelectSteeringMode: (SteeringMode) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AsphaltNavy
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "RACE CONTROLS & SETTINGS",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )

            // Sound Toggle
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = PitSurface,
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = NitroCyan)
                        Column {
                            Text("Engine & Turbo Audio", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("Synthesized RPM & exhaust sound", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                    Switch(
                        checked = state.settings.soundEnabled,
                        onCheckedChange = onToggleSound,
                        colors = SwitchDefaults.colors(checkedThumbColor = TextPrimary, checkedTrackColor = RacingCrimson)
                    )
                }
            }

            // Haptics Toggle
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = PitSurface,
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.Vibration, contentDescription = null, tint = ChampionshipAmber)
                        Column {
                            Text("Tactile Haptics", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("Vibration on nitro, near-miss & bumps", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                    Switch(
                        checked = state.settings.hapticsEnabled,
                        onCheckedChange = onToggleHaptics,
                        colors = SwitchDefaults.colors(checkedThumbColor = TextPrimary, checkedTrackColor = RacingCrimson)
                    )
                }
            }

            // Steering Mode
            Text("STEERING METHOD", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            SteeringMode.entries.forEach { mode ->
                val selected = state.settings.steeringMode == mode
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSteeringMode(mode) },
                    shape = RoundedCornerShape(12.dp),
                    color = PitSurface,
                    border = BorderStroke(1.dp, if (selected) RacingCrimson else SubtleBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onSelectSteeringMode(mode) },
                            colors = RadioButtonDefaults.colors(selectedColor = RacingCrimson)
                        )
                        Column {
                            Text(mode.label, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(mode.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                }
            }

            // AdMob & Play Store Monetization Status
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = PitSurface,
                border = BorderStroke(1.dp, ChampionshipAmber.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.MonetizationOn, contentDescription = null, tint = ChampionshipAmber)
                        Text(
                            text = if (state.isUsingTestAdIds) "AdMob Monetization (Test IDs Active)"
                            else "AdMob Monetization (Live Production)",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChampionshipAmber
                        )
                    }
                    Text(
                        text = "Banner, Interstitial, and Rewarded Video ads are integrated. Before uploading your .aab to Google Play Console, replace the 4 admob_* IDs in res/values/strings.xml with your AdMob Console IDs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = if (state.isOnline) "Network: Online • Ad SDK ready" else "Network: Offline • Full offline gameplay active",
                        style = MaterialTheme.typography.labelSmall,
                        color = TelemetryEmerald
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
