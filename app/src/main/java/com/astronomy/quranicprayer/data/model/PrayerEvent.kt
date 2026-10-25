package com.astronomy.quranicprayer.data.model

import java.time.LocalDate
import java.time.ZonedDateTime

enum class PrayerEvent(
    val title: String,
    val arabicTerm: String,
    val astronomicalAnchor: String
) {
    FAJR(
        title = "Fajr",
        arabicTerm = "الفجر",
        astronomicalAnchor = "Zon op -6.0° → Zonsopkomst"
    ),
    DULUK(
        title = "Duluk al-Shams",
        arabicTerm = "دلوك الشمس",
        astronomicalAnchor = "Zenith → Zonsondergang"
    ),
    GHASAQ(
        title = "Ghasaq al-Layl",
        arabicTerm = "غسق الليل",
        astronomicalAnchor = "Zon op -6.0° → -12.0°"
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
