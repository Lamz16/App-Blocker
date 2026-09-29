package com.example.firewall

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.FirewallPreferences
import com.example.data.model.FirewallLogEntity
import com.example.domain.model.AppNetworkRule
import com.example.domain.model.NetworkType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.net.InetAddress
import java.nio.ByteBuffer

class NetworkVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.example.firewall.START"
        const val ACTION_STOP = "com.example.firewall.STOP"
        const val ACTION_UPDATE_RULES = "com.example.firewall.UPDATE_RULES"

        private const val NOTIFICATION_CHANNEL_ID = "netblocker_firewall_channel"
        private const val NOTIFICATION_ID = 1001

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning = _isServiceRunning.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, NetworkVpnService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, NetworkVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun updateRules(context: Context) {
            val intent = Intent(context, NetworkVpnService::class.java).apply {
                action = ACTION_UPDATE_RULES
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var vpnInterface: ParcelFileDescriptor? = null
    private var packetWorkerJob: Job? = null
    private var networkMonitorJob: Job? = null

    private lateinit var database: AppDatabase
    private lateinit var preferences: FirewallPreferences
    private lateinit var networkMonitor: NetworkMonitor
    private val ruleManager = FirewallRuleManager()

    private val lastLoggedTimes = mutableMapOf<String, Long>()

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(applicationContext)
        preferences = FirewallPreferences(applicationContext)
        networkMonitor = NetworkMonitor(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                shutdownVpn()
                return START_NOT_STICKY
            }
            ACTION_UPDATE_RULES -> {
                serviceScope.launch {
                    reloadVpnRules()
                }
                return START_STICKY
            }
            ACTION_START, null -> {
                startForegroundWithNotification("Initializing Protection...", 0)
                _isServiceRunning.value = true
                serviceScope.launch {
                    preferences.setFirewallEnabled(true)
                    observeNetworkAndRules()
                }
                return START_STICKY
            }
        }
        return START_STICKY
    }

    private fun observeNetworkAndRules() {
        networkMonitorJob?.cancel()
        networkMonitorJob = serviceScope.launch {
            networkMonitor.networkType.collectLatest {
                reloadVpnRules()
            }
        }
    }

    private suspend fun reloadVpnRules() {
        val rules = database.appRuleDao().getAllRules().map {
            AppNetworkRule(
                packageName = it.packageName,
                appName = it.appName,
                isBlocked = it.isBlocked,
                blockWifi = it.blockWifi,
                blockMobileData = it.blockMobileData,
                blockBackground = it.blockBackground
            )
        }
        val currentNetwork = networkMonitor.determineCurrentNetworkType()
        val blockedPackages = ruleManager.computeBlockedPackages(
            rules = rules,
            firewallEnabled = true,
            networkType = currentNetwork
        )

        updateNotification(blockedPackages.size, currentNetwork)
        setupVpnInterface(blockedPackages)
    }

    private fun setupVpnInterface(blockedPackages: Set<String>) {
        try {
            packetWorkerJob?.cancel()
            vpnInterface?.close()
            vpnInterface = null

            val builder = Builder()
                .setSession("NetBlocker Firewall")
                .setMtu(1500)
                .addAddress("10.254.1.1", 32)
                .addRoute("0.0.0.0", 0)
                .addAddress("fd00:1::1", 128)
                .addRoute("::", 0)
                .addDnsServer("10.254.1.1")

            val pm = packageManager
            var addedCount = 0

            // Route ONLY the blocked applications into this local drop VPN tunnel
            for (pkg in blockedPackages) {
                try {
                    pm.getPackageInfo(pkg, 0)
                    builder.addAllowedApplication(pkg)
                    addedCount++
                } catch (_: PackageManager.NameNotFoundException) {
                    // Ignored if app is no longer installed
                }
            }

            // If no apps are currently blocked, add our own app as a dummy blackhole target
            // so the VPN remains established and visible in status bar without affecting user apps.
            if (addedCount == 0) {
                builder.addAllowedApplication(packageName)
            }

            vpnInterface = builder.establish()

            vpnInterface?.let { pfd ->
                startPacketConsumer(pfd, blockedPackages)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startPacketConsumer(pfd: ParcelFileDescriptor, blockedPackages: Set<String>) {
        packetWorkerJob = serviceScope.launch(Dispatchers.IO) {
            val inStream = FileInputStream(pfd.fileDescriptor)
            val packet = ByteBuffer.allocate(32768)

            try {
                while (_isServiceRunning.value && !Thread.interrupted()) {
                    packet.clear()
                    val length = inStream.read(packet.array())
                    if (length > 0) {
                        packet.limit(length)
                        inspectAndDropPacket(packet, blockedPackages)
                    }
                }
            } catch (_: Exception) {
                // Closed or interrupted
            }
        }
    }

    private fun inspectAndDropPacket(buffer: ByteBuffer, blockedPackages: Set<String>) {
        try {
            if (buffer.remaining() < 20) return
            val versionAndIhl = buffer.get(0).toInt()
            val version = (versionAndIhl shr 4) and 0x0F

            if (version == 4) {
                val protocolNum = buffer.get(9).toInt() and 0xFF
                val protocolStr = when (protocolNum) {
                    6 -> "TCP"
                    17 -> "UDP"
                    1 -> "ICMP"
                    else -> "IPv4($protocolNum)"
                }

                val destIpBytes = ByteArray(4)
                buffer.position(16)
                buffer.get(destIpBytes)
                val destIp = InetAddress.getByAddress(destIpBytes).hostAddress ?: "Unknown"

                var destPort: Int? = null
                if (protocolNum == 6 || protocolNum == 17) {
                    val ihl = (versionAndIhl and 0x0F) * 4
                    if (buffer.limit() >= ihl + 4) {
                        destPort = buffer.getShort(ihl + 2).toInt() and 0xFFFF
                    }
                }

                // Log drop rate-limited
                val targetApp = blockedPackages.firstOrNull() ?: "Blocked App"
                val now = System.currentTimeMillis()
                val lastLogged = lastLoggedTimes[targetApp] ?: 0L
                if (now - lastLogged > 2000L) {
                    lastLoggedTimes[targetApp] = now
                    serviceScope.launch {
                        database.firewallLogDao().insertLog(
                            FirewallLogEntity(
                                packageName = targetApp,
                                appName = targetApp,
                                action = "BLOCKED",
                                networkType = networkMonitor.determineCurrentNetworkType().name,
                                destinationIp = destIp,
                                destinationPort = destPort,
                                protocol = protocolStr
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun shutdownVpn() {
        _isServiceRunning.value = false
        packetWorkerJob?.cancel()
        networkMonitorJob?.cancel()

        try {
            vpnInterface?.close()
        } catch (_: Exception) {
        }
        vpnInterface = null

        serviceScope.launch {
            preferences.setFirewallEnabled(false)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Network Guard Firewall Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the active status of local network protection"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, NetworkVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Network Guard Active")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.netblocker_icon_1790685762789)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Firewall",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun startForegroundWithNotification(statusText: String, blockedCount: Int) {
        val notification = buildNotification(statusText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(blockedCount: Int, networkType: NetworkType) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val text = "$blockedCount apps blocked on ${networkType.name.lowercase().replaceFirstChar { it.uppercase() }}"
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
        networkMonitor.unregister()
        serviceScope.cancel()
        try {
            vpnInterface?.close()
        } catch (_: Exception) {
        }
        vpnInterface = null
    }
}
