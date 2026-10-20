package com.lockin.app.hardware.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.os.Build
import androidx.core.content.getSystemService
import com.lockin.app.domain.model.TriggerProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

data class WifiZoneInfo(
    val isConnected: Boolean,
    val ssid: String? = null,
    val bssid: String? = null,
)

@Singleton
class WifiZoneDetector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val connectivityManager = context.getSystemService<ConnectivityManager>()

    /**
     * Emits real-time Wi-Fi connection info including SSID and BSSID.
     * Uses Android 12+ (API 31+) NetworkCallback(FLAG_INCLUDE_LOCATION_INFO).
     */
    fun observeCurrentWifi(): Flow<WifiZoneInfo> = callbackFlow {
        val cm = connectivityManager ?: run {
            trySend(WifiZoneInfo(isConnected = false))
            close()
            return@callbackFlow
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                override fun onAvailable(network: Network) {
                    val caps = cm.getNetworkCapabilities(network)
                    val info = extractWifiInfo(caps)
                    trySend(info)
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    val info = extractWifiInfo(networkCapabilities)
                    trySend(info)
                }

                override fun onLost(network: Network) {
                    trySend(WifiZoneInfo(isConnected = false))
                }
            }
        } else {
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val caps = cm.getNetworkCapabilities(network)
                    val info = extractWifiInfo(caps)
                    trySend(info)
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    val info = extractWifiInfo(networkCapabilities)
                    trySend(info)
                }

                override fun onLost(network: Network) {
                    trySend(WifiZoneInfo(isConnected = false))
                }
            }
        }

        cm.registerNetworkCallback(request, callback)

        // Emit initial state if available
        val activeNetwork = cm.activeNetwork
        val caps = if (activeNetwork != null) cm.getNetworkCapabilities(activeNetwork) else null
        if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            trySend(extractWifiInfo(caps))
        } else {
            trySend(WifiZoneInfo(isConnected = false))
        }

        awaitClose {
            cm.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    private fun extractWifiInfo(capabilities: NetworkCapabilities?): WifiZoneInfo {
        if (capabilities == null || !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return WifiZoneInfo(isConnected = false)
        }

        val transportInfo = capabilities.transportInfo
        val wifiInfo = transportInfo as? WifiInfo

        var ssid = wifiInfo?.ssid?.removeSurrounding("\"")
        if (ssid == "<unknown ssid>" || ssid.isNullOrBlank()) {
            ssid = null
        }

        var bssid = wifiInfo?.bssid
        if (bssid == "02:00:00:00:00:00" || bssid.isNullOrBlank()) {
            bssid = null
        }

        return WifiZoneInfo(
            isConnected = true,
            ssid = ssid,
            bssid = bssid,
        )
    }

    /**
     * Checks whether the current Wi-Fi matches any registered Study Zone trigger profile (by BSSID or SSID).
     */
    fun findMatchingZone(
        currentWifi: WifiZoneInfo,
        savedZones: List<TriggerProfile>,
    ): TriggerProfile? {
        if (!currentWifi.isConnected) return null

        return savedZones.firstOrNull { zone ->
            val zoneValue = zone.value.trim()
            val matchesBssid = !currentWifi.bssid.isNullOrBlank() &&
                    zoneValue.equals(currentWifi.bssid, ignoreCase = true)
            val matchesSsid = !currentWifi.ssid.isNullOrBlank() &&
                    zoneValue.equals(currentWifi.ssid, ignoreCase = true)

            matchesBssid || matchesSsid
        }
    }
}

