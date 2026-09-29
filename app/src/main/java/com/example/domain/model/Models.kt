package com.example.domain.model

enum class NetworkType {
    WIFI,
    MOBILE,
    DISCONNECTED,
    OTHER
}

enum class AppFilter {
    ALL,
    BLOCKED,
    ALLOWED,
    USER_APPS,
    SYSTEM_APPS
}

data class AppNetworkRule(
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean = false,
    val blockWifi: Boolean = false,
    val blockMobileData: Boolean = false,
    val blockBackground: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val rule: AppNetworkRule,
    val downloadBytes: Long = 0L,
    val uploadBytes: Long = 0L,
    val totalBytes: Long = 0L
)

data class FirewallLog(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val appName: String,
    val action: String,
    val networkType: String,
    val destinationIp: String? = null,
    val destinationPort: Int? = null,
    val protocol: String? = null
)

data class FirewallDashboardStats(
    val isProtectionActive: Boolean = false,
    val blockedAppsCount: Int = 0,
    val allowedAppsCount: Int = 0,
    val totalAppsCount: Int = 0,
    val totalBytesDownloaded: Long = 0L,
    val totalBytesUploaded: Long = 0L,
    val totalBytesSavedEstimate: Long = 0L,
    val activeNetworkType: NetworkType = NetworkType.DISCONNECTED
)
