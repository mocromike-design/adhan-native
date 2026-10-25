package com.astronomy.quranicprayer.data.model

import java.time.LocalDate
import java.time.ZonedDateTime

enum class PrayerEvent(
    val title: String,
    val astronomicalAnchor: String
) {
    FAJR(
        title = "Fajr",
        astronomicalAnchor = "Zon op -6.0° tot Zonsopkomst"
    ),
    DULUK(
        title = "Duluk al-Shams",
        astronomicalAnchor = "Zenith tot Zonsondergang"
    ),
    GHASAQ(
        title = "Ghasaq al-Layl",
        astronomicalAnchor = "Zon op -6.0° tot -12.0°"
    )
}

data class EventEntry(
    val event: PrayerEvent,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime
)

data class DailySchedule(
    val date: LocalDate,
    val entries: List<EventEntry>,
    val locationName: String,
    val latitude: Double,
    val longitude: Double
)
