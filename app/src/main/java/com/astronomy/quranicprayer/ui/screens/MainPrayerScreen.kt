package com.astronomy.quranicprayer.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
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
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val scheduler = remember { PrayerAlarmScheduler(context) }
    var hasExactAlarmPermission by remember { mutableStateOf(scheduler.canScheduleExactAlarms()) }

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
                    Spacer(modifier = Modifier.height(24.dp))

                    if (!hasExactAlarmPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ExactAlarmBanner(onClick = {
                            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                        })
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 1. Top Countdown
                    CountdownSection(
                        nextEvent = uiState.nextEvent,
                        countdown = uiState.countdownFormatted
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // 2. Middle Location Indicator
                    LocationSection(
                        locationName = uiState.schedule?.locationName ?: "Active GPS",
                        onRefresh = onRefresh
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // 3. The 3 Astronomical Times
                    ScheduleList(
                        entries = uiState.schedule?.entries ?: emptyList(),
                        nextEvent = uiState.nextEvent
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
            text = (nextEvent?.event?.title ?: "NEXT TRANSITION").uppercase(),
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
            text = nextEvent?.event?.astronomicalAnchor ?: "Calculating solar transition",
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

@Composable
fun LocationSection(locationName: String, onRefresh: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(32.dp))
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = TextSecondary,
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
                contentDescription = "Refresh GPS",
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
fun ExactAlarmBanner(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2C1600))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = AccentSolar)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Tik hier om Exacte Wekkers toe te staan",
            fontSize = 12.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onClick) {
            Text("Inschakelen", color = AccentSolar, fontSize = 12.sp)
        }
    }
}
