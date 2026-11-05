package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import java.util.Locale

data class LiveGpsReading(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val provider: String,
    val addressLabel: String,
    val timestampMillis: Long
)

object RealTimeLocationAndMediaHelper {

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun resolveAddressSync(context: Context, lat: Double, lng: Double): String {
        return try {
            @Suppress("DEPRECATION")
            val list = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lng, 1)
            val first = list?.firstOrNull()
            if (first != null) {
                val locality = first.subLocality ?: first.locality ?: first.subAdminArea
                val admin = first.locality ?: first.adminArea ?: first.countryName
                listOfNotNull(locality, admin).distinct().joinToString(", ").ifBlank {
                    String.format(Locale.US, "%.5f, %.5f", lat, lng)
                }
            } else {
                String.format(Locale.US, "%.5f, %.5f", lat, lng)
            }
        } catch (_: Exception) {
            String.format(Locale.US, "%.5f, %.5f", lat, lng)
        }
    }

    @SuppressLint("MissingPermission")
    @Composable
    fun rememberLiveGpsLocation(context: Context, enabled: Boolean): State<LiveGpsReading?> {
        val state = remember { mutableStateOf<LiveGpsReading?>(null) }

        DisposableEffect(context, enabled) {
            if (!enabled || !hasLocationPermission(context)) {
                onDispose { }
            } else {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val listener = LocationListener { loc: Location ->
                    val addr = resolveAddressSync(context, loc.latitude, loc.longitude)
                    state.value = LiveGpsReading(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracyMeters = loc.accuracy,
                        provider = loc.provider ?: "GPS",
                        addressLabel = addr,
                        timestampMillis = loc.time
                    )
                }

                try {
                    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
                    for (provider in providers) {
                        if (lm?.isProviderEnabled(provider) == true) {
                            val last = lm.getLastKnownLocation(provider)
                            if (last != null && state.value == null) {
                                state.value = LiveGpsReading(
                                    latitude = last.latitude,
                                    longitude = last.longitude,
                                    accuracyMeters = last.accuracy,
                                    provider = last.provider ?: provider,
                                    addressLabel = resolveAddressSync(context, last.latitude, last.longitude),
                                    timestampMillis = last.time
                                )
                            }
                            lm.requestLocationUpdates(provider, 3000L, 2f, listener, Looper.getMainLooper())
                        }
                    }
                } catch (_: Exception) {
                    // Ignore if hardware provider unavailable
                }

                onDispose {
                    try {
                        lm?.removeUpdates(listener)
                    } catch (_: Exception) {
                    }
                }
            }
        }

        return state
    }

    fun loadImageBitmapFromUri(context: Context, uriString: String?): ImageBitmap? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = 2
                }
                BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }
}
