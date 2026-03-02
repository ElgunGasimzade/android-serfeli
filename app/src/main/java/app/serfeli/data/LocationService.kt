package app.serfeli.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LocationState(
    val lat: Double? = null,
    val lon: Double? = null,
    val range: Double = 1.0, // Default 1km
    val isEnabled: Boolean = true,
    val permissionGranted: Boolean = false
)

class LocationService(
    private val context: Context,
    private val sessionManager: SessionManager
) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val prefs = context.getSharedPreferences("daily_deals_prefs", Context.MODE_PRIVATE)

    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _locationState.update { it.copy(lat = location.latitude, lon = location.longitude) }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "search_range" -> updateStateFromPrefs()
            "location_enabled" -> {
                updateStateFromPrefs()
                refreshLocationUpdates()
            }
        }
    }

    init {
        // Initial load
        updateStateFromPrefs()
        refreshLocationUpdates()
        
        // Listen for settings changes
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    fun checkForPermissions() {
        val fineLocation = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        val granted = fineLocation == android.content.pm.PackageManager.PERMISSION_GRANTED || 
                      coarseLocation == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (granted != _locationState.value.permissionGranted) {
             _locationState.update { it.copy(permissionGranted = granted) }
             refreshLocationUpdates()
        }
    }

    private fun updateStateFromPrefs() {
        val range = sessionManager.getSearchRange()
        val enabled = sessionManager.isLocationEnabled()
        _locationState.update { it.copy(range = range.toDouble(), isEnabled = enabled) }
    }

    @SuppressLint("MissingPermission")
    private fun refreshLocationUpdates() {
        // Check permissions again just in case
        val fineLocation = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        val hasPermission = fineLocation == android.content.pm.PackageManager.PERMISSION_GRANTED || 
                            coarseLocation == android.content.pm.PackageManager.PERMISSION_GRANTED

        _locationState.update { it.copy(permissionGranted = hasPermission) }

        if (hasPermission && _locationState.value.isEnabled) {
            try {
                // Get last known location immediately
                val lastKnownGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val lastKnownNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                val location = lastKnownGPS ?: lastKnownNetwork
                
                if (location != null) {
                    _locationState.update { it.copy(lat = location.latitude, lon = location.longitude) }
                }

                // Request updates
                // Min time: 5 seconds, Min distance: 50 meters
                locationManager.removeUpdates(locationListener) // Remove first to avoid duplicates
                
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                     locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 50f, locationListener, Looper.getMainLooper())
                }
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                     locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 50f, locationListener, Looper.getMainLooper())
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Stop updates AND clear location data
            locationManager.removeUpdates(locationListener)
            _locationState.update { it.copy(lat = null, lon = null) }
        }
    }

    fun cleanup() {
        locationManager.removeUpdates(locationListener)
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }
}
