package com.astronomy.quranicprayer.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.astronomy.quranicprayer.data.model.DailySchedule
import java.time.ZonedDateTime

class PrayerAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarms(schedule: DailySchedule) {
        if (!canScheduleExactAlarms()) return

        val now = ZonedDateTime.now()

        // 1. Schedule exact alarms for each prayer event (Fajr, Duluk, Ghasaq)
        schedule.entries.forEachIndexed { index, entry ->
            if (entry.time.isAfter(now)) {
                val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    action = PrayerAlarmReceiver.ACTION_TRIGGER_PRAYER
                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, entry.event.title)
                    putExtra(PrayerAlarmReceiver.EXTRA_ASTRONOMICAL_INFO, entry.event.astronomicalAnchor)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    index + 100,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val triggerMillis = entry.time.toInstant().toEpochMilli()
                
                // setExactAndAllowWhileIdle ensures it fires even in Android Deep Doze mode
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        }

        // 2. Schedule Midnight Recalculation Trigger (00:01 AM tomorrow)
        val tomorrowMidnight = now.toLocalDate()
            .plusDays(1)
            .atTime(0, 1)
            .atZone(now.zone)

        val midnightIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_MIDNIGHT_RESCHEDULE
            putExtra("lat", schedule.latitude)
            putExtra("lon", schedule.longitude)
        }

        val midnightPendingIntent = PendingIntent.getBroadcast(
            context,
            999,
            midnightIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            tomorrowMidnight.toInstant().toEpochMilli(),
            midnightPendingIntent
        )
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
