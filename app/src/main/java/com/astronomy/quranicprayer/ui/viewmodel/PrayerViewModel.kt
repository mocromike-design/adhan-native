package com.astronomy.quranicprayer.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astronomy.quranicprayer.data.location.LocationTracker
import com.astronomy.quranicprayer.data.model.DailySchedule
import com.astronomy.quranicprayer.data.model.EventEntry
import com.astronomy.quranicprayer.data.model.PrayerEvent
import com.astronomy.quranicprayer.domain.astronomy.SolarCalculator
import com.astronomy.quranicprayer.notification.PrayerAlarmScheduler
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class PrayerUiState(
    val isLoading: Boolean = true,
    val schedule: DailySchedule? = null,
    val nextEvent: EventEntry? = null,
    val countdownFormatted: String = "00:00:00",
    val error: String? = null
)

class PrayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PrayerUiState())
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    private val locationTracker = LocationTracker(
        LocationServices.getFusedLocationProviderClient(application),
        application
    )
    private val scheduler = PrayerAlarmScheduler(application)

    init {
        startCountdownTicker()
    }

    fun loadSchedule() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val location = locationTracker.getCurrentLocation()

            if (location == null) {
                // If location not obtained, fall back to Amsterdam or cached coordinates
                val prefs = getApplication<Application>().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val lat = prefs.getFloat("last_lat", 52.3676f).toDouble()
                val lon = prefs.getFloat("last_lon", 4.9041f).toDouble()
                computeScheduleWithCoords(lat, lon, "Amsterdam / Default")
                return@launch
            }

            val prefs = getApplication<Application>().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putFloat("last_lat", location.latitude.toFloat())
                .putFloat("last_lon", location.longitude.toFloat())
                .apply()

            val cityName = locationTracker.getCityName(location.latitude, location.longitude)
            computeScheduleWithCoords(location.latitude, location.longitude, cityName)
        }
    }

    private fun computeScheduleWithCoords(latitude: Double, longitude: Double, cityName: String) {
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()

        val fajr = SolarCalculator.calculateTimeAtAltitude(today, latitude, longitude, -18.0, true, zoneId)
        val duluk = SolarCalculator.calculateSolarNoon(today, latitude, longitude, zoneId)
        val ghasaq = SolarCalculator.calculateTimeAtAltitude(today, latitude, longitude, -0.833, false, zoneId)

        val entries = mutableListOf<EventEntry>()
        fajr?.let { entries.add(EventEntry(PrayerEvent.FAJR, it)) }
        entries.add(EventEntry(PrayerEvent.DULUK, duluk))
        ghasaq?.let { entries.add(EventEntry(PrayerEvent.GHASAQ, it)) }

        val schedule = DailySchedule(
            date = today,
            entries = entries,
            locationName = cityName,
            latitude = latitude,
            longitude = longitude
        )

        // Arms the exact alarms in Android AlarmManager
        scheduler.scheduleAlarms(schedule)

        _uiState.update {
            it.copy(
                isLoading = false,
                schedule = schedule,
                error = null
            )
        }
        updateUpcomingEvent()
    }

    private fun startCountdownTicker() {
        viewModelScope.launch {
            while (isActive) {
                updateUpcomingEvent()
                delay(1000)
            }
        }
    }

    private fun updateUpcomingEvent() {
        val schedule = _uiState.value.schedule ?: return
        val now = ZonedDateTime.now()

        val next = schedule.entries.firstOrNull { it.time.isAfter(now) }
            ?: run {
                val tomorrow = LocalDate.now().plusDays(1)
                val zoneId = ZoneId.systemDefault()
                val tomorrowFajr = SolarCalculator.calculateTimeAtAltitude(
                    tomorrow,
                    schedule.latitude,
                    schedule.longitude,
                    -18.0,
                    true,
                    zoneId
                ) ?: schedule.entries.first().time.plusDays(1)
                EventEntry(PrayerEvent.FAJR, tomorrowFajr)
            }

        val duration = Duration.between(now, next.time)
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60

        val formatted = "%02d:%02d:%02d".format(hours, minutes, seconds)

        _uiState.update {
            it.copy(
                nextEvent = next,
                countdownFormatted = formatted
            )
        }
    }
}
