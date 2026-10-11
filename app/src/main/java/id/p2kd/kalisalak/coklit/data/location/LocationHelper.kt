package id.p2kd.kalisalak.coklit.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun getHighAccuracyLocation(
        context: Context,
        onResult: (lat: Double?, lng: Double?, accuracy: Float?) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            onResult(null, null, null)
            return
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            onResult(null, null, null)
            return
        }

        // 1. Coba ambil last known location terbaik terlebih dahulu (cepat)
        var bestLastLocation: Location? = null
        try {
            val gpsLast = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val netLast = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val passiveLast = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

            val candidates = listOfNotNull(gpsLast, netLast, passiveLast)
            if (candidates.isNotEmpty()) {
                bestLastLocation = candidates.minByOrNull { it.accuracy }
            }
        } catch (_: Exception) {}

        // Jika last location sangat akurat (< 25 meter) dan masih baru (< 60 detik), bisa langsung digunakan
        if (bestLastLocation != null && bestLastLocation.accuracy <= 25f && (System.currentTimeMillis() - bestLastLocation.time) < 60000L) {
            onResult(bestLastLocation.latitude, bestLastLocation.longitude, bestLastLocation.accuracy)
            return
        }

        // 2. Minta pembaruan realtime lokasi satelit GPS (Tingkat Presisi Tinggi)
        var hasReceivedUpdate = false
        val handler = Handler(Looper.getMainLooper())

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (!hasReceivedUpdate) {
                    hasReceivedUpdate = true
                    try {
                        locationManager.removeUpdates(this)
                    } catch (_: Exception) {}
                    onResult(location.latitude, location.longitude, location.accuracy)
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    500L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    500L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            }
        } catch (_: Exception) {
            if (!hasReceivedUpdate) {
                hasReceivedUpdate = true
                onResult(bestLastLocation?.latitude, bestLastLocation?.longitude, bestLastLocation?.accuracy)
                return
            }
        }

        // Timeout 6 detik: jika satelit GPS butuh waktu lama, gunakan best last location
        handler.postDelayed({
            if (!hasReceivedUpdate) {
                hasReceivedUpdate = true
                try {
                    locationManager.removeUpdates(listener)
                } catch (_: Exception) {}
                onResult(bestLastLocation?.latitude, bestLastLocation?.longitude, bestLastLocation?.accuracy)
            }
        }, 6000L)
    }
}
