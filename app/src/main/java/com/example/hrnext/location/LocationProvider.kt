package com.example.hrnext.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/** One-shot device location fetch, used both by the check-in/out button and the foreground
 * service's periodic pings. Plain [LocationManager] — no Play Services dependency. */
object LocationProvider {

    suspend fun getCurrentLocation(context: Context, timeoutMs: Long = 15_000): Location? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val provider = when {
            runCatching { manager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false) -> LocationManager.GPS_PROVIDER
            runCatching { manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false) -> LocationManager.NETWORK_PROVIDER
            else -> return lastKnownLocation(manager)
        }

        val fresh = withTimeoutOrNull(timeoutMs) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) fetchModern(manager, provider) else fetchLegacy(manager, provider)
        }
        return fresh ?: lastKnownLocation(manager, provider)
    }

    private fun lastKnownLocation(manager: LocationManager, provider: String? = null): Location? {
        val providers = listOfNotNull(provider, LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        return providers.firstNotNullOfOrNull { p -> runCatching { manager.getLastKnownLocation(p) }.getOrNull() }
    }

    private suspend fun fetchModern(manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            val executor = Executors.newSingleThreadExecutor()
            cont.invokeOnCancellation { signal.cancel() }
            runCatching {
                manager.getCurrentLocation(provider, signal, executor) { location ->
                    if (cont.isActive) cont.resume(location)
                }
            }.onFailure { if (cont.isActive) cont.resume(null) }
        }

    private suspend fun fetchLegacy(manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { cont ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (cont.isActive) cont.resume(location)
                }
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            cont.invokeOnCancellation { runCatching { manager.removeUpdates(listener) } }
            runCatching {
                @Suppress("DEPRECATION")
                manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            }.onFailure { if (cont.isActive) cont.resume(null) }
        }
}
