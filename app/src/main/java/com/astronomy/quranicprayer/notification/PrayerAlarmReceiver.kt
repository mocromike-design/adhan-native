package com.astronomy.quranicprayer.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.astronomy.quranicprayer.MainActivity
import com.astronomy.quranicprayer.R
import com.astronomy.quranicprayer.data.model.DailySchedule
import com.astronomy.quranicprayer.data.model.EventEntry
import com.astronomy.quranicprayer.data.model.PrayerEvent
import com.astronomy.quranicprayer.domain.astronomy.SolarCalculator
import java.time.LocalDate
import java.time.ZoneId

class PrayerAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_PRAYER = "com.astronomy.quranicprayer.ACTION_TRIGGER_PRAYER"
        const val ACTION_MIDNIGHT_RESCHEDULE = "com.astronomy.quranicprayer.ACTION_MIDNIGHT_RESCHEDULE"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_ASTRONOMICAL_INFO = "extra_astronomical_info"
        private const val CHANNEL_ID = "celestial_adhan_channel_v2"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "QuranicPrayer::AlarmWakeLock"
        )
        wakeLock.acquire(30000L) // Hold wake lock for 30s max

        try {
            when (intent.action) {
                ACTION_TRIGGER_PRAYER -> {
                    val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Celestial Transition"
                    val astroInfo = intent.getStringExtra(EXTRA_ASTRONOMICAL_INFO) ?: "Solar Alignment"
                    
                    showNotificationWithVibrationAndAudio(context, prayerName, astroInfo)
                }
                ACTION_MIDNIGHT_RESCHEDULE -> {
                    val lat = intent.getDoubleExtra("lat", 0.0)
                    val lon = intent.getDoubleExtra("lon", 0.0)
                    if (lat != 0.0 && lon != 0.0) {
                        val today = LocalDate.now()
                        val zone = ZoneId.systemDefault()
                        val fajr = SolarCalculator.calculateTimeAtAltitude(today, lat, lon, -18.0, true, zone)
                        val duluk = SolarCalculator.calculateSolarNoon(today, lat, lon, zone)
                        val ghasaq = SolarCalculator.calculateTimeAtAltitude(today, lat, lon, -0.833, false, zone)

                        val entries = mutableListOf<EventEntry>()
                        fajr?.let { entries.add(EventEntry(PrayerEvent.FAJR, it)) }
                        entries.add(EventEntry(PrayerEvent.DULUK, duluk))
                        ghasaq?.let { entries.add(EventEntry(PrayerEvent.GHASAQ, it)) }

                        val schedule = DailySchedule(today, entries, "Auto Rescheduled", lat, lon)
                        PrayerAlarmScheduler(context).scheduleAlarms(schedule)
                    }
                }
            }
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    private fun showNotificationWithVibrationAndAudio(context: Context, title: String, content: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val adhanUri = Uri.parse("android.resource://${context.packageName}/${R.raw.adhan}")
        val vibrationPattern = longArrayOf(0, 1000, 400, 1000, 400, 1000)

        // Create Channel on Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Adhan Prayer Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Nidā' Astronomical Prayer Alarms with Sound & Vibration"
                enableVibration(true)
                setVibrationPattern(vibrationPattern)
                setSound(adhanUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Check Phone Silent / Vibration State
        val ringerMode = audioManager.ringerMode
        if (ringerMode == AudioManager.RINGER_MODE_VIBRATE || ringerMode == AudioManager.RINGER_MODE_SILENT) {
            // Phone is on Mute or Vibrate: Vibrate explicitly via Vibrator hardware
            vibrateDevice(context, vibrationPattern)
        } else {
            // Phone is on Normal Mode: Play Adhan media audio explicitly to ensure it recites even if screen is locked
            playAdhanMedia(context)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Nidā': $title")
            .setContentText("Solar Alignment: $content")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVibrate(vibrationPattern)
            .setSound(adhanUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun vibrateDevice(context: Context, pattern: LongArray) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playAdhanMedia(context: Context) {
        try {
            val player = MediaPlayer.create(context, R.raw.adhan)
            player.setOnCompletionListener { it.release() }
            player.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
