package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.data.local.RaceRecordEntity
import com.example.model.RaceMode
import com.example.model.RacingCatalog
import com.example.ui.MainUiState
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
fun RecordsScreen(
    state: MainUiState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "CAREER TELEMETRY",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CareerMetricTile(
                        label = "RACES",
                        value = "${state.profile.totalRaces}",
                        icon = Icons.Rounded.Flag,
                        accent = NitroCyan,
                        modifier = Modifier.weight(1f)
                    )
                    CareerMetricTile(
                        label = "WINS / GOLD",
                        value = "${state.profile.totalWins}",
                        icon = Icons.Rounded.EmojiEvents,
                        accent = ChampionshipAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CareerMetricTile(
                        label = "DISTANCE",
                        value = String.format("%.1f KM", state.profile.totalDistanceKm),
                        icon = Icons.Rounded.Route,
                        accent = TelemetryEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    CareerMetricTile(
                        label = "HIGH SCORE",
                        value = "${state.profile.bestEndlessScore}",
                        icon = Icons.Rounded.Speed,
                        accent = RacingCrimson,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "RECENT OFFLINE RUNS",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
            }

            if (state.recentRecords.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = PitSurface,
                        border = BorderStroke(1.dp, SubtleBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Flag,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No race logs yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Complete any City Sprint, Time Attack, or Endless Highway run to save local telemetry.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(state.recentRecords, key = { it.id }) { rec ->
                    RaceHistoryCard(record = rec)
                }
            }
        }
    }
}

@Composable
private fun CareerMetricTile(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = PitSurface,
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Text(value, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        }
    }
}

@Composable
private fun RaceHistoryCard(record: RaceRecordEntity) {
    val track = RacingCatalog.trackById(record.trackId)
    val car = RacingCatalog.carById(record.carId)
    val driver = RacingCatalog.driverById(record.driverId)
    val modeTitle = RaceMode.entries.firstOrNull { it.id == record.modeId }?.title ?: "Race"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PitSurfaceElevated),
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "$modeTitle • ${track.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "${car.name} • ${driver.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = "Top ${record.topSpeedKmh} km/h • ${record.nearMisses} Near Misses",
                    style = MaterialTheme.typography.labelSmall,
                    color = NitroCyan
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (record.modeId == "endless_highway") "${record.score} PTS"
                    else "P${record.position} • ${formatRaceTime(record.elapsedTimeMs)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (record.position == 1) ChampionshipAmber else TextPrimary
                )
                Text(
                    text = "+${record.creditsEarned} CR",
                    style = MaterialTheme.typography.labelSmall,
                    color = TelemetryEmerald
                )
            }
        }
    }
}

fun formatRaceTime(ms: Long): String {
    val totalSec = (ms / 1000).toInt()
    val min = totalSec / 60
    val sec = totalSec % 60
    val hundredths = ((ms % 1000) / 10).toInt()
    return String.format("%02d:%02d.%02d", min, sec, hundredths)
}
