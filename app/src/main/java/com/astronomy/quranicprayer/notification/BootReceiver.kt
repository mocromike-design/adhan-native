package com.astronomy.quranicprayer.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val lat = prefs.getFloat("last_lat", 0f).toDouble()
            val lon = prefs.getFloat("last_lon", 0f).toDouble()

            if (lat != 0.0 && lon != 0.0) {
                val triggerIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    action = PrayerAlarmReceiver.ACTION_MIDNIGHT_RESCHEDULE
                    putExtra("lat", lat)
                    putExtra("lon", lon)
                }
                context.sendBroadcast(triggerIntent)
            }
        }
    }
}
