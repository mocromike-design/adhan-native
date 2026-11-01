package com.astronomy.quranicprayer.data.model

import java.time.LocalDate
import java.time.ZonedDateTime

enum class PrayerEvent(
    val title: String,
    val astronomicalAnchor: String
) {
    FAJR(
        title = "Salaat al-Fajr",
        astronomicalAnchor = "-6 graden oost"
    ),
    WUSTA(
        title = "Salaat al-Wusta",
        astronomicalAnchor = "Zenith"
    ),
    ISHA(
        title = "Salaat al-Isha",
        astronomicalAnchor = "0 graden west"
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
