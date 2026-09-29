package lamz.netblocker.firewall

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import lamz.netblocker.domain.model.NetworkType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _networkType = MutableStateFlow(determineCurrentNetworkType())
    val networkType: StateFlow<NetworkType> = _networkType.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateNetworkType()
        }

        override fun onLost(network: Network) {
            updateNetworkType()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            updateNetworkType()
        }
    }

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (_: Exception) {
            // Gracefully handle if registration fails
        }
    }

    fun updateNetworkType() {
        _networkType.value = determineCurrentNetworkType()
    }

    fun determineCurrentNetworkType(): NetworkType {
        val cm = connectivityManager ?: return NetworkType.DISCONNECTED
        val activeNetwork = cm.activeNetwork ?: return NetworkType.DISCONNECTED
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkType.DISCONNECTED

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.WIFI
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> NetworkType.OTHER
            else -> NetworkType.DISCONNECTED
        }
    }

    fun unregister() {
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
        }
    }
}
