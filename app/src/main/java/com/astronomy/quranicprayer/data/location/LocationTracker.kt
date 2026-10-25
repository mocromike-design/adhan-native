package com.astronomy.quranicprayer.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationTracker(
    private val client: FusedLocationProviderClient,
    private val context: Context
) {
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        val cts = CancellationTokenSource()
        return try {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token).await()
                ?: client.lastLocation.await()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCityName(latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var name = String.format(Locale.US, "%.2f°, %.2f°", latitude, longitude)
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    addresses.firstOrNull()?.let {
                        name = it.locality ?: it.subAdminArea ?: it.countryName ?: name
                    }
                }
                name
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()?.let {
                    it.locality ?: it.subAdminArea ?: it.countryName
                } ?: String.format(Locale.US, "%.2f°, %.2f°", latitude, longitude)
            }
        } catch (e: Exception) {
            String.format(Locale.US, "%.2f°, %.2f°", latitude, longitude)
        }
    }
}
