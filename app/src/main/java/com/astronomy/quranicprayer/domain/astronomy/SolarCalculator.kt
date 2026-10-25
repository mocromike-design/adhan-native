package com.astronomy.quranicprayer.domain.astronomy

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.*

object SolarCalculator {

    private const val RAD = Math.PI / 180.0
    private const val DEG = 180.0 / Math.PI

    fun calculateSolarNoon(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId
    ): ZonedDateTime {
        val jd = toJulianDay(date, 12.0)
        val t = (jd - 2451545.0) / 36525.0
        val eot = equationOfTime(t)
        
        val noonUtcMinutes = 720.0 - (4.0 * longitude) - eot
        return minutesToZonedDateTime(date, noonUtcMinutes, zoneId)
    }

    fun calculateTimeAtAltitude(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        targetAltitudeDeg: Double,
        isMorning: Boolean,
        zoneId: ZoneId
    ): ZonedDateTime? {
        val jd = toJulianDay(date, 12.0)
        val t = (jd - 2451545.0) / 36525.0
        val declination = solarDeclination(t)
        val eot = equationOfTime(t)

        val latRad = latitude * RAD
        val decRad = declination * RAD
        val altRad = targetAltitudeDeg * RAD

        var cosHourAngle = (sin(altRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))

        // High-latitude safe clamp
        if (cosHourAngle > 1.0) cosHourAngle = 1.0
        if (cosHourAngle < -1.0) cosHourAngle = -1.0

        val hourAngleDeg = acos(cosHourAngle) * DEG
        val hourOffsetMinutes = 4.0 * hourAngleDeg

        val noonUtcMinutes = 720.0 - (4.0 * longitude) - eot
        val eventUtcMinutes = if (isMorning) {
            noonUtcMinutes - hourOffsetMinutes
        } else {
            noonUtcMinutes + hourOffsetMinutes
        }

        return minutesToZonedDateTime(date, eventUtcMinutes, zoneId)
    }

    private fun toJulianDay(date: LocalDate, hourOfDay: Double): Double {
        var y = date.year
        var m = date.monthValue
        val d = date.dayOfMonth.toDouble() + (hourOfDay / 24.0)

        if (m <= 2) {
            y -= 1
            m += 12
        }

        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + d + b - 1524.5
    }

    private fun equationOfTime(t: Double): Double {
        val epsilon = (23.439291 - 0.0130042 * t) * RAD
        val l0 = (280.46646 + 36000.76983 * t) % 360.0 * RAD
        val m = (357.52911 + 35999.05029 * t) % 360.0 * RAD
        val e = 0.016708634 - 0.000042037 * t

        val y = tan(epsilon / 2.0).pow(2)
        val sin2L0 = sin(2.0 * l0)
        val sinM = sin(m)
        val cos2L0 = cos(2.0 * l0)
        val sin4L0 = sin(4.0 * l0)
        val sin2M = sin(2.0 * m)

        val eotRad = y * sin2L0 - 2.0 * e * sinM + 4.0 * e * y * sinM * cos2L0 - 0.5 * y * y * sin4L0 - 1.25 * e * e * sin2M
        return eotRad * DEG * 4.0
    }

    private fun solarDeclination(t: Double): Double {
        val l0 = (280.46646 + 36000.76983 * t) % 360.0 * RAD
        val m = (357.52911 + 35999.05029 * t) % 360.0 * RAD
        val c = (1.914602 - 0.004817 * t) * sin(m) + (0.019993 - 0.000101 * t) * sin(2.0 * m) + 0.000289 * sin(3.0 * m)
        val trueLong = l0 + (c * RAD)
        val epsilon = (23.439291 - 0.0130042 * t) * RAD
        return asin(sin(epsilon) * sin(trueLong)) * DEG
    }

    private fun minutesToZonedDateTime(date: LocalDate, minutesUtc: Double, zoneId: ZoneId): ZonedDateTime {
        val utcSeconds = (minutesUtc * 60.0).roundToLong()
        val utcMidnight = date.atStartOfDay(ZoneId.of("UTC"))
        val eventUtc = utcMidnight.plusSeconds(utcSeconds)
        return eventUtc.withZoneSameInstant(zoneId)
    }
}
