package com.xiaoquexing.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

data class LocatedPlace(
    val name: String,
    val lat: Double,
    val lng: Double,
)

object PlaceLocator {
    fun hasPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    suspend fun current(context: Context): LocatedPlace? = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) return@withContext null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@withContext null
        val last = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .filter { lm.isProviderEnabled(it) || it == LocationManager.PASSIVE_PROVIDER }
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        val fresh = if (last != null && System.currentTimeMillis() - last.time < 3 * 60_000L) {
            last
        } else {
            withTimeoutOrNull(8_000) { awaitFix(lm) } ?: last
        } ?: return@withContext null
        val name = reverse(context, fresh) ?: "当前位置"
        LocatedPlace(name = name, lat = fresh.latitude, lng = fresh.longitude)
    }

    private suspend fun awaitFix(lm: LocationManager): Location? =
        suspendCancellableCoroutine { cont ->
            val provider = when {
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                else -> {
                    cont.resume(null)
                    return@suspendCancellableCoroutine
                }
            }
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    runCatching { lm.removeUpdates(this) }
                    if (cont.isActive) cont.resume(location)
                }
            }
            runCatching {
                lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            }.onFailure {
                if (cont.isActive) cont.resume(null)
            }
            cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
        }

    private fun reverse(context: Context, location: Location): String? = runCatching {
        if (!Geocoder.isPresent()) return@runCatching null
        val addr = Geocoder(context, Locale.CHINA)
            .getFromLocation(location.latitude, location.longitude, 1)
            ?.firstOrNull() ?: return@runCatching null
        listOfNotNull(addr.subLocality, addr.thoroughfare, addr.featureName, addr.locality)
            .firstOrNull { it.isNotBlank() && it != addr.countryName }
    }.getOrNull()
}
