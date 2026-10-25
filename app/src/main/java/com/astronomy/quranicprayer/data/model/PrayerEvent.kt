package com.astronomy.quranicprayer.data.model

import java.time.LocalDate
import java.time.ZonedDateTime

enum class PrayerEvent(
    val title: String,
    val arabicTerm: String,
    val astronomicalAnchor: String,
    val targetSunAngle: Double
) {
    FAJR(
        title = "Fajr",
        arabicTerm = "الفجر",
        astronomicalAnchor = "Astronomical Dawn (-18.0°)",
        targetSunAngle = -18.0
    ),
    DULUK(
        title = "Duluk al-Shams",
        arabicTerm = "دلوك الشمس",
        astronomicalAnchor = "Solar Zenith (Culmination)",
        targetSunAngle = 0.0
    ),
    GHASAQ(
        title = "Ghasaq al-Layl",
        arabicTerm = "غسق الليل",
        astronomicalAnchor = "Sunset / Horizon (-0.833°)",
        targetSunAngle = -0.833
    )
}

data class EventEntry(
    val event: PrayerEvent,
    val time: ZonedDateTime
)

data class DailySchedule(
    val date: LocalDate,
    val entries: List<EventEntry>,
    val locationName: String,
    val latitude: Double,
    val longitude: Double
)
