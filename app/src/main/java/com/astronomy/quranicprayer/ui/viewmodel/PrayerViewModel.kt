package com.astronomy.quranicprayer.ui.viewmodel

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astronomy.quranicprayer.R
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
    val error: String? = null,
    val isAdhanEnabled: Boolean = true,
    val isTestPlaying: Boolean = false
)

class PrayerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        PrayerUiState(
            isAdhanEnabled = prefs.getBoolean("adhan_enabled", true)
        )
    )
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    private val locationTracker = LocationTracker(
        LocationServices.getFusedLocationProviderClient(application),
        application
    )
    private val scheduler = PrayerAlarmScheduler(application)
    private var testPlayer: MediaPlayer? = null

    init {
        startCountdownTicker()
    }

    fun loadSchedule() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val location = locationTracker.getCurrentLocation()

            if (location == null) {
                // If location not obtained, fall back to Hechtel-Eksel or cached coordinates
                val lat = prefs.getFloat("last_lat", 51.1270f).toDouble()
                val lon = prefs.getFloat("last_lon", 5.3740f).toDouble()
                val city = prefs.getString("last_city", "Hechtel-Eksel") ?: "Hechtel-Eksel"
                computeScheduleWithCoords(lat, lon, city)
                return@launch
            }

            val cityName = locationTracker.getCityName(location.latitude, location.longitude)
            prefs.edit()
                .putFloat("last_lat", location.latitude.toFloat())
                .putFloat("last_lon", location.longitude.toFloat())
                .putString("last_city", cityName)
                .apply()

            computeScheduleWithCoords(location.latitude, location.longitude, cityName)
        }
    }

    fun setCustomCoordinates(latitude: Double, longitude: Double, cityName: String) {
        prefs.edit()
            .putFloat("last_lat", latitude.toFloat())
            .putFloat("last_lon", longitude.toFloat())
            .putString("last_city", cityName)
            .apply()
        computeScheduleWithCoords(latitude, longitude, cityName)
    }

    fun toggleAdhan() {
        val newState = !_uiState.value.isAdhanEnabled
        prefs.edit().putBoolean("adhan_enabled", newState).apply()
        if (!newState && _uiState.value.isTestPlaying) {
            stopTestAdhan()
        }
        _uiState.update { it.copy(isAdhanEnabled = newState) }
    }

    fun togglePlayTestAdhan() {
        if (_uiState.value.isTestPlaying) {
            stopTestAdhan()
        } else {
            startTestAdhan()
        }
    }

    private fun startTestAdhan() {
        try {
            stopTestAdhan()
            testPlayer = MediaPlayer.create(getApplication(), R.raw.adhan).apply {
                setOnCompletionListener {
                    _uiState.update { state -> state.copy(isTestPlaying = false) }
                    release()
                    testPlayer = null
                }
                start()
            }
            _uiState.update { it.copy(isTestPlaying = true) }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(isTestPlaying = false) }
        }
    }

    private fun stopTestAdhan() {
        try {
            testPlayer?.stop()
            testPlayer?.release()
            testPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _uiState.update { it.copy(isTestPlaying = false) }
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

    override fun onCleared() {
        super.onCleared()
        stopTestAdhan()
    }
}
