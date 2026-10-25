package com.astronomy.quranicprayer.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astronomy.quranicprayer.data.model.EventEntry
import com.astronomy.quranicprayer.notification.PrayerAlarmScheduler
import com.astronomy.quranicprayer.ui.theme.*
import com.astronomy.quranicprayer.ui.viewmodel.PrayerUiState
import java.time.format.DateTimeFormatter

@Composable
fun MainPrayerScreen(
    uiState: PrayerUiState,
    onRefresh: () -> Unit,
    onToggleAdhan: () -> Unit,
    onToggleTestAdhan: () -> Unit,
    onSetLocation: (Double, Double, String) -> Unit
) {
    val context = LocalContext.current
    val scheduler = remember { PrayerAlarmScheduler(context) }
    var hasExactAlarmPermission by remember { mutableStateOf(scheduler.canScheduleExactAlarms()) }
    var showLocationDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onRefresh()
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    if (showLocationDialog) {
        LocationSelectorDialog(
            currentCity = uiState.schedule?.locationName ?: "Amsterdam",
            currentLat = uiState.schedule?.latitude ?: 52.3676,
            currentLon = uiState.schedule?.longitude ?: 4.9041,
            onDismiss = { showLocationDialog = false },
            onApply = { lat, lon, city ->
                onSetLocation(lat, lon, city)
                showLocationDialog = false
            },
            onUseGps = {
                onRefresh()
                showLocationDialog = false
            }
        )
    }

    Scaffold(
        containerColor = BackgroundDark
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AccentSolar,
                    strokeWidth = 2.dp
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // 1. Top Countdown Section
                    CountdownSection(
                        nextEvent = uiState.nextEvent,
                        countdown = uiState.countdownFormatted
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 2. Middle: Location Pill (Clickable to change city / coords)
                    LocationSection(
                        locationName = uiState.schedule?.locationName ?: "Actieve Locatie",
                        onClick = { showLocationDialog = true },
                        onRefresh = onRefresh
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // 3. The 3 Astronomical Times
                    ScheduleList(
                        entries = uiState.schedule?.entries ?: emptyList(),
                        nextEvent = uiState.nextEvent
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // 4. Quick Action Controls: Adhan ON/OFF & Test Adhan
                    ActionControlsRow(
                        isAdhanEnabled = uiState.isAdhanEnabled,
                        isTestPlaying = uiState.isTestPlaying,
                        onToggleAdhan = onToggleAdhan,
                        onToggleTestAdhan = onToggleTestAdhan
                    )
                }
            }
        }
    }
}

@Composable
fun CountdownSection(nextEvent: EventEntry?, countdown: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = (nextEvent?.event?.title ?: "VOLGENDE OVERGANG").uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 3.sp,
            color = AccentSolar
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = countdown,
            fontSize = 58.sp,
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily.Monospace,
            letterSpacing = (-1).sp,
            color = TextPrimary
        )

        Text(
            text = nextEvent?.event?.astronomicalAnchor ?: "Berekenen astronomische stand",
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

@Composable
fun LocationSection(
    locationName: String,
    onClick: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(32.dp))
            .background(SurfaceDark)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = AccentSolar,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = locationName,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onRefresh,
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "GPS Vernieuwen",
                tint = TextSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun ScheduleList(entries: List<EventEntry>, nextEvent: EventEntry?) {
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        entries.forEach { entry ->
            val isNext = entry.event == nextEvent?.event
            val borderColor = if (isNext) AccentSolar.copy(alpha = 0.6f) else Color.Transparent
            val cardBackground = if (isNext) SurfaceHighlight else SurfaceDark

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBackground)
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entry.event.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = entry.event.arabicTerm,
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = entry.event.astronomicalAnchor,
                        fontSize = 11.sp,
                        color = if (isNext) AccentSolar else TextSecondary
                    )
                }

                Text(
                    text = entry.time.format(formatter),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    color = if (isNext) AccentSolar else TextPrimary
                )
            }
        }
    }
}

@Composable
fun ActionControlsRow(
    isAdhanEnabled: Boolean,
    isTestPlaying: Boolean,
    onToggleAdhan: () -> Unit,
    onToggleTestAdhan: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Toggle Adhan ON/OFF Button
        Button(
            onClick = onToggleAdhan,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAdhanEnabled) SurfaceHighlight else SurfaceDark
            ),
            shape = RoundedCornerShape(14.dp),
            border = if (isAdhanEnabled) ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(AccentSolar)) else null
        ) {
            Icon(
                imageVector = if (isAdhanEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                contentDescription = null,
                tint = if (isAdhanEnabled) AccentSolar else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isAdhanEnabled) "Adhan AAN" else "Adhan UIT",
                fontSize = 12.sp,
                color = if (isAdhanEnabled) TextPrimary else TextSecondary
            )
        }

        // Test Adhan Play/Stop Button
        Button(
            onClick = onToggleTestAdhan,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isTestPlaying) SurfaceHighlight else SurfaceDark
            ),
            shape = RoundedCornerShape(14.dp),
            border = if (isTestPlaying) ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(AccentSolar)) else null
        ) {
            Icon(
                imageVector = if (isTestPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isTestPlaying) AccentSolar else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isTestPlaying) "Stop Adhan" else "Test Adhan",
                fontSize = 12.sp,
                color = if (isTestPlaying) AccentSolar else TextPrimary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectorDialog(
    currentCity: String,
    currentLat: Double,
    currentLon: Double,
    onDismiss: () -> Unit,
    onApply: (Double, Double, String) -> Unit,
    onUseGps: () -> Unit
) {
    var latText by remember { mutableStateOf(String.format(java.util.Locale.US, "%.4f", currentLat)) }
    var lonText by remember { mutableStateOf(String.format(java.util.Locale.US, "%.4f", currentLon)) }

    val presetCities = remember {
        listOf(
            Triple("Hechtel-Eksel", 51.1270, 5.3740),
            Triple("Amsterdam", 52.3676, 4.9041),
            Triple("Rotterdam", 51.9244, 4.4777),
            Triple("Den Haag", 52.0705, 4.3007),
            Triple("Utrecht", 52.0907, 5.1214),
            Triple("Brussel", 50.8503, 4.3517),
            Triple("Antwerpen", 51.2194, 4.4025),
            Triple("Londen", 51.5074, -0.1278),
            Triple("Parijs", 48.8566, 2.3522),
            Triple("Berlijn", 52.5200, 13.4050),
            Triple("Istanbul", 41.0082, 28.9784),
            Triple("Mekka", 21.4225, 39.8262),
            Triple("Medina", 24.5247, 39.5692),
            Triple("Jeruzalem", 31.7683, 35.2137),
            Triple("Caïro", 30.0444, 31.2357),
            Triple("Dubai", 25.2048, 55.2708)
        )
    }

    var selectedPreset by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "Locatie Instellingen",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Kies een stad of voer handmatig coördinaten in:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                // Preset selector chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onUseGps,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentSolar),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("📍 Gebruik Satelliet GPS", color = BackgroundDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "Of snelle stadskeuze:",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                // Simple preset buttons row (most common)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        listOf(Triple("Hechtel-Eksel", 51.1270, 5.3740), Triple("Brussel", 50.8503, 4.3517)),
                        listOf(Triple("Antwerpen", 51.2194, 4.4025), Triple("Amsterdam", 52.3676, 4.9041)),
                        listOf(Triple("Rotterdam", 51.9244, 4.4777), Triple("Den Haag", 52.0705, 4.3007)),
                        listOf(Triple("Utrecht", 52.0907, 5.1214), Triple("Mekka", 21.4225, 39.8262))
                    ).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { (city, lat, lon) ->
                                OutlinedButton(
                                    onClick = {
                                        latText = String.format(java.util.Locale.US, "%.4f", lat)
                                        lonText = String.format(java.util.Locale.US, "%.4f", lon)
                                        selectedPreset = city
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selectedPreset == city) SurfaceHighlight else Color.Transparent
                                    )
                                ) {
                                    Text(city, fontSize = 11.sp, color = TextPrimary)
                                }
                            }
                        }
                    }
                }

                // Manual Coords
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("Breedtegraad", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = lonText,
                        onValueChange = { lonText = it },
                        label = { Text("Lengtegraad", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latText.toDoubleOrNull() ?: currentLat
                    val lon = lonText.toDoubleOrNull() ?: currentLon
                    val name = if (selectedPreset.isNotEmpty()) selectedPreset else String.format(java.util.Locale.US, "%.2f°, %.2f°", lat, lon)
                    onApply(lat, lon, name)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentSolar)
            ) {
                Text("Toepassen", color = BackgroundDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuleren", color = TextSecondary)
            }
        }
    )
}
