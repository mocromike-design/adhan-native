package com.astronomy.quranicprayer.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astronomy.quranicprayer.data.model.EventEntry
import com.astronomy.quranicprayer.ui.theme.*
import com.astronomy.quranicprayer.ui.viewmodel.PrayerUiState
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MainPrayerScreen(
    uiState: PrayerUiState,
    onRefresh: () -> Unit,
    onToggleAdhan: () -> Unit,
    onToggleTestAdhan: () -> Unit,
    onSetLocation: (Double, Double, String) -> Unit
) {
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
            currentCity = uiState.schedule?.locationName ?: "Hechtel-Eksel",
            currentLat = uiState.schedule?.latitude ?: 51.1270,
            currentLon = uiState.schedule?.longitude ?: 5.3740,
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

    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                BackgroundDark,
                BackgroundMid,
                BackgroundBottom
            )
        )
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(paddingValues)
        ) {
            // Adaptive sizing for compact screens (e.g. Samsung S24) vs large screens (e.g. S26 Ultra)
            val isCompactScreen = maxHeight < 760.dp
            val verticalSpacing = if (isCompactScreen) 10.dp else 14.dp
            val cardPaddingVertical = if (isCompactScreen) 12.dp else 16.dp
            val countdownFontSize = if (isCompactScreen) 38.sp else 44.sp
            val intervalFontSize = if (isCompactScreen) 21.sp else 24.sp

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AccentSolar,
                    strokeWidth = 2.5.dp
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = if (isCompactScreen) 8.dp else 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Group: Location Bar + Countdown Card
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LocationHeader(
                            locationName = uiState.schedule?.locationName ?: "Hechtel-Eksel",
                            onClick = { showLocationDialog = true },
                            onRefresh = onRefresh
                        )

                        Spacer(modifier = Modifier.height(verticalSpacing))

                        CountdownHeroCard(
                            nextEvent = uiState.nextEvent,
                            countdown = uiState.countdownFormatted,
                            countdownFontSize = countdownFontSize,
                            isCompact = isCompactScreen
                        )
                    }

                    Spacer(modifier = Modifier.height(verticalSpacing))

                    // Middle Group: The 3 Prayer Period Boxes
                    ScheduleCardsSection(
                        entries = uiState.schedule?.entries ?: emptyList(),
                        nextEvent = uiState.nextEvent,
                        cardSpacing = verticalSpacing,
                        cardPaddingVertical = cardPaddingVertical,
                        intervalFontSize = intervalFontSize
                    )

                    Spacer(modifier = Modifier.height(verticalSpacing))

                    // Bottom Group: Adhan Controls
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
fun LocationHeader(
    locationName: String,
    onClick: () -> Unit,
    onRefresh: () -> Unit
) {
    val pillBorderBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(
                AccentCyanGlow.copy(alpha = 0.6f),
                AccentSolar.copy(alpha = 0.7f),
                AccentCyanGlow.copy(alpha = 0.6f)
            )
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .shadow(6.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(SurfaceCardTop, SurfaceDark, SurfaceCardTop)
                )
            )
            .border(1.dp, pillBorderBrush, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 7.dp)
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
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(AccentSubtle.copy(alpha = 0.6f))
                .clickable { onRefresh() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "GPS Vernieuwen",
                tint = TextCyan,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
fun CountdownHeroCard(
    nextEvent: EventEntry?,
    countdown: String,
    countdownFontSize: androidx.compose.ui.unit.TextUnit,
    isCompact: Boolean
) {
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    val heroBorderBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                AccentSolar.copy(alpha = 0.85f),
                AccentCyanGlow.copy(alpha = 0.55f),
                AccentSolar.copy(alpha = 0.85f)
            )
        )
    }

    val heroBgBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                SurfaceHighlight.copy(alpha = 0.9f),
                SurfaceDark.copy(alpha = 0.95f)
            )
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(heroBgBrush)
            .border(1.5.dp, heroBorderBrush, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = if (isCompact) 10.dp else 14.dp)
    ) {
        val headerText = nextEvent?.let {
            "VOLGENDE PERIODE: ${it.event.title.uppercase()} (${it.startTime.format(formatter)})"
        } ?: "VOLGENDE PERIODE"

        Text(
            text = headerText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = AccentGoldBright,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = countdown,
            fontSize = countdownFontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Text(
            text = nextEvent?.event?.astronomicalAnchor ?: "Astronomische zonnestand berekenen...",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextCyan,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ScheduleCardsSection(
    entries: List<EventEntry>,
    nextEvent: EventEntry?,
    cardSpacing: Dp,
    cardPaddingVertical: Dp,
    intervalFontSize: androidx.compose.ui.unit.TextUnit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val now = ZonedDateTime.now()

    Column(
        verticalArrangement = Arrangement.spacedBy(cardSpacing),
        modifier = Modifier.fillMaxWidth()
    ) {
        entries.forEach { entry ->
            val isActiveNow = now.isAfter(entry.startTime) && now.isBefore(entry.endTime)
            val isNextUpcoming = entry.event == nextEvent?.event && !isActiveNow

            val cardBorderBrush = when {
                isActiveNow -> Brush.linearGradient(
                    colors = listOf(AccentSolar, AccentCyanGlow, AccentSolar)
                )
                isNextUpcoming -> Brush.linearGradient(
                    colors = listOf(
                        AccentSolar.copy(alpha = 0.75f),
                        AccentCyan.copy(alpha = 0.6f),
                        AccentSolar.copy(alpha = 0.75f)
                    )
                )
                else -> Brush.linearGradient(
                    colors = listOf(
                        AccentCyan.copy(alpha = 0.45f),
                        AccentSubtle.copy(alpha = 0.8f),
                        AccentSolar.copy(alpha = 0.35f)
                    )
                )
            }

            val cardBgBrush = when {
                isActiveNow -> Brush.verticalGradient(
                    colors = listOf(SurfaceHighlight, SurfaceHighlightBottom)
                )
                isNextUpcoming -> Brush.verticalGradient(
                    colors = listOf(SurfaceCardTop, SurfaceCardBottom)
                )
                else -> Brush.verticalGradient(
                    colors = listOf(SurfaceDark, SurfaceCardBottom)
                )
            }

            val borderWidth = if (isActiveNow) 2.dp else 1.3.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(if (isActiveNow) 12.dp else 6.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBgBrush)
                    .border(borderWidth, cardBorderBrush, RoundedCornerShape(20.dp))
            ) {
                // Subtle top decorative highlight bar
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(0.45f)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    if (isActiveNow || isNextUpcoming) AccentSolar else AccentCyanGlow.copy(alpha = 0.7f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = cardPaddingVertical),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Boven in het midden: De naam van de tijd
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = entry.event.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = if (isActiveNow || isNextUpcoming) AccentGoldBright else TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        if (isActiveNow) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = AccentSolar.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(50),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, AccentSolar)
                            ) {
                                Text(
                                    text = "ACTIEF",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                    color = AccentSolar,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 2. Daaronder in het midden: Het tijdsvak
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(BackgroundDark.copy(alpha = 0.45f))
                            .border(
                                0.8.dp,
                                if (isActiveNow || isNextUpcoming) AccentSolar.copy(alpha = 0.35f)
                                else AccentCyan.copy(alpha = 0.2f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 18.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${entry.startTime.format(formatter)}  –  ${entry.endTime.format(formatter)}",
                            fontSize = intervalFontSize,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isActiveNow) AccentSolar else TextPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 3. Daaronder in het midden: De zonstanden
                    Text(
                        text = entry.event.astronomicalAnchor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isActiveNow || isNextUpcoming) TextCyan else TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
        val adhanBorder = if (isAdhanEnabled) {
            Brush.horizontalGradient(listOf(AccentSolar, AccentCyanGlow))
        } else {
            Brush.horizontalGradient(listOf(AccentSubtle, AccentSubtle))
        }

        Button(
            onClick = onToggleAdhan,
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .border(1.2.dp, adhanBorder, RoundedCornerShape(14.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAdhanEnabled) SurfaceHighlight else SurfaceDark
            ),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) {
            Icon(
                imageVector = if (isAdhanEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                contentDescription = null,
                tint = if (isAdhanEnabled) AccentSolar else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isAdhanEnabled) "Adhan AAN" else "Adhan UIT",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isAdhanEnabled) TextPrimary else TextSecondary,
                maxLines = 1
            )
        }

        // Test Adhan Play/Stop Button
        val testBorder = if (isTestPlaying) {
            Brush.horizontalGradient(listOf(AccentSolar, AccentAmber))
        } else {
            Brush.horizontalGradient(listOf(AccentCyan.copy(alpha = 0.5f), AccentSubtle))
        }

        Button(
            onClick = onToggleTestAdhan,
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .border(1.2.dp, testBorder, RoundedCornerShape(14.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isTestPlaying) SurfaceHighlight else SurfaceDark
            ),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) {
            Icon(
                imageVector = if (isTestPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isTestPlaying) AccentSolar else TextCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isTestPlaying) "Stop Adhan" else "Test Adhan",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isTestPlaying) AccentSolar else TextPrimary,
                maxLines = 1
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
    var latText by remember { mutableStateOf(String.format(Locale.US, "%.4f", currentLat)) }
    var lonText by remember { mutableStateOf(String.format(Locale.US, "%.4f", currentLon)) }
    var selectedPreset by remember { mutableStateOf(currentCity) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = "Locatie Instellingen",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGoldBright
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onUseGps,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentSolar),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "📍 Gebruik Satelliet GPS",
                        color = BackgroundDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Snelle stadskeuze:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextCyan
                )

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
                                val isSelected = selectedPreset == city
                                OutlinedButton(
                                    onClick = {
                                        latText = String.format(Locale.US, "%.4f", lat)
                                        lonText = String.format(Locale.US, "%.4f", lon)
                                        selectedPreset = city
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) AccentSolar else AccentCyan.copy(alpha = 0.35f)
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) SurfaceHighlight else BackgroundDark.copy(alpha = 0.4f)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = city,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) AccentGoldBright else TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Handmatige coördinaten:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextCyan
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = {
                            latText = it
                            selectedPreset = ""
                        },
                        label = { Text("Breedtegraad", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = lonText,
                        onValueChange = {
                            lonText = it
                            selectedPreset = ""
                        },
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
                    val name = if (selectedPreset.isNotEmpty()) selectedPreset else String.format(Locale.US, "%.2f°, %.2f°", lat, lon)
                    onApply(lat, lon, name)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentSolar),
                shape = RoundedCornerShape(10.dp)
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
