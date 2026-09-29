package lamz.netblocker.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import lamz.netblocker.data.local.AppRuleDao
import lamz.netblocker.data.local.FirewallLogDao
import lamz.netblocker.data.local.FirewallPreferences
import lamz.netblocker.data.model.AppNetworkRuleEntity
import lamz.netblocker.data.model.FirewallLogEntity
import lamz.netblocker.domain.model.AppNetworkRule
import lamz.netblocker.domain.model.FirewallDashboardStats
import lamz.netblocker.domain.model.FirewallLog
import lamz.netblocker.domain.model.InstalledApp
import lamz.netblocker.domain.model.NetworkType
import lamz.netblocker.firewall.NetworkMonitor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FirewallRepository(
    private val context: Context,
    private val appRuleDao: AppRuleDao,
    private val firewallLogDao: FirewallLogDao,
    private val preferences: FirewallPreferences,
    private val networkMonitor: NetworkMonitor,
    private val usageStatsHelper: UsageStatsHelper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    val isFirewallEnabledFlow: Flow<Boolean> = preferences.isFirewallEnabledFlow
    val showSystemAppsFlow: Flow<Boolean> = preferences.showSystemAppsFlow
    val blockByDefaultFlow: Flow<Boolean> = preferences.blockByDefaultFlow
    val blockBackgroundGlobalFlow: Flow<Boolean> = preferences.blockBackgroundGlobalFlow
    val adBlockingEnabledFlow: Flow<Boolean> = preferences.adBlockingEnabledFlow
    val activeNetworkTypeFlow: Flow<NetworkType> = networkMonitor.networkType

    suspend fun setFirewallEnabled(enabled: Boolean) {
        preferences.setFirewallEnabled(enabled)
    }

    suspend fun setShowSystemApps(show: Boolean) {
        preferences.setShowSystemApps(show)
    }

    suspend fun setBlockByDefault(block: Boolean) {
        preferences.setBlockByDefault(block)
    }

    suspend fun setBlockBackgroundGlobal(block: Boolean) {
        preferences.setBlockBackgroundGlobal(block)
    }

    suspend fun setAdBlockingEnabled(enabled: Boolean) {
        preferences.setAdBlockingEnabled(enabled)
    }

    fun getLogsFlow(): Flow<List<FirewallLog>> {
        return firewallLogDao.getRecentLogsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun insertLog(log: FirewallLog) = withContext(ioDispatcher) {
        firewallLogDao.insertLog(log.toEntity())
    }

    suspend fun clearLogs() = withContext(ioDispatcher) {
        firewallLogDao.clearAllLogs()
    }

    fun getInstalledAppsFlow(): Flow<List<InstalledApp>> {
        return combine(
            appRuleDao.getAllRulesFlow(),
            showSystemAppsFlow
        ) { rules, showSystemApps ->
            withContext(ioDispatcher) {
                val packageManager = context.packageManager
                val installedPackages = try {
                    packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                } catch (_: Exception) {
                    emptyList<ApplicationInfo>()
                }

                val ruleMap = rules.associateBy { it.packageName }
                val ourPackageName = context.packageName

                val resultList = mutableListOf<InstalledApp>()

                for (appInfo in installedPackages) {
                    // Do not block ourselves
                    if (appInfo.packageName == ourPackageName) continue

                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    if (isSystem && !showSystemApps) continue

                    val appName = try {
                        packageManager.getApplicationLabel(appInfo).toString()
                    } catch (_: Exception) {
                        appInfo.packageName
                    }

                    val existingRuleEntity = ruleMap[appInfo.packageName]
                    val rule = existingRuleEntity?.toDomain() ?: AppNetworkRule(
                        packageName = appInfo.packageName,
                        appName = appName,
                        isBlocked = false,
                        blockWifi = false,
                        blockMobileData = false,
                        blockBackground = false
                    )

                    val (rx, tx) = usageStatsHelper.getAppUsageToday(appInfo.packageName)
                    resultList.add(
                        InstalledApp(
                            packageName = appInfo.packageName,
                            appName = appName,
                            isSystemApp = isSystem,
                            rule = rule,
                            downloadBytes = rx,
                            uploadBytes = tx,
                            totalBytes = rx + tx
                        )
                    )
                }

                resultList.sortedWith(
                    compareByDescending<InstalledApp> { it.rule.isBlocked }
                        .thenBy { it.appName.lowercase() }
                )
            }
        }
    }

    suspend fun toggleBlock(packageName: String, isBlocked: Boolean) = withContext(ioDispatcher) {
        val existing = appRuleDao.getRuleByPackage(packageName)
        val appName = getAppName(packageName)
        val updated = existing?.copy(isBlocked = isBlocked, updatedAt = System.currentTimeMillis())
            ?: AppNetworkRuleEntity(
                packageName = packageName,
                appName = appName,
                isBlocked = isBlocked
            )
        appRuleDao.upsertRule(updated)

        // Log the action
        firewallLogDao.insertLog(
            FirewallLogEntity(
                packageName = packageName,
                appName = appName,
                action = if (isBlocked) "BLOCKED" else "ALLOWED",
                networkType = networkMonitor.determineCurrentNetworkType().name
            )
        )
    }

    suspend fun updateRule(rule: AppNetworkRule) = withContext(ioDispatcher) {
        appRuleDao.upsertRule(rule.toEntity())
    }

    suspend fun toggleBlockWifi(packageName: String, blockWifi: Boolean) = withContext(ioDispatcher) {
        val existing = appRuleDao.getRuleByPackage(packageName)
        val appName = getAppName(packageName)
        val updated = existing?.copy(blockWifi = blockWifi, updatedAt = System.currentTimeMillis())
            ?: AppNetworkRuleEntity(
                packageName = packageName,
                appName = appName,
                blockWifi = blockWifi
            )
        appRuleDao.upsertRule(updated)
    }

    suspend fun toggleBlockMobileData(packageName: String, blockMobileData: Boolean) = withContext(ioDispatcher) {
        val existing = appRuleDao.getRuleByPackage(packageName)
        val appName = getAppName(packageName)
        val updated = existing?.copy(blockMobileData = blockMobileData, updatedAt = System.currentTimeMillis())
            ?: AppNetworkRuleEntity(
                packageName = packageName,
                appName = appName,
                blockMobileData = blockMobileData
            )
        appRuleDao.upsertRule(updated)
    }

    suspend fun toggleBlockBackground(packageName: String, blockBackground: Boolean) = withContext(ioDispatcher) {
        val existing = appRuleDao.getRuleByPackage(packageName)
        val appName = getAppName(packageName)
        val updated = existing?.copy(blockBackground = blockBackground, updatedAt = System.currentTimeMillis())
            ?: AppNetworkRuleEntity(
                packageName = packageName,
                appName = appName,
                blockBackground = blockBackground
            )
        appRuleDao.upsertRule(updated)
    }

    suspend fun deleteRule(packageName: String) = withContext(ioDispatcher) {
        appRuleDao.deleteRuleByPackage(packageName)
    }

    suspend fun getAllRules(): List<AppNetworkRule> = withContext(ioDispatcher) {
        appRuleDao.getAllRules().map { it.toDomain() }
    }

    fun getDashboardStatsFlow(): Flow<FirewallDashboardStats> {
        return combine(
            isFirewallEnabledFlow,
            appRuleDao.getAllRulesFlow(),
            networkMonitor.networkType
        ) { isEnabled, rules, activeNetwork ->
            val blockedCount = rules.count { it.isBlocked || it.blockWifi || it.blockMobileData }
            val (totalRx, totalTx) = usageStatsHelper.getTotalUsageToday()
            val totalUsage = totalRx + totalTx
            // Estimate saved data based on blocked apps ratio (or ~20-30% of total)
            val savedEstimate = if (blockedCount > 0) (blockedCount * 28L * 1024L * 1024L) else 0L

            FirewallDashboardStats(
                isProtectionActive = isEnabled,
                blockedAppsCount = blockedCount,
                allowedAppsCount = (rules.size - blockedCount).coerceAtLeast(0),
                totalAppsCount = rules.size,
                totalBytesDownloaded = totalRx,
                totalBytesUploaded = totalTx,
                totalBytesSavedEstimate = savedEstimate,
                activeNetworkType = activeNetwork
            )
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    private fun AppNetworkRuleEntity.toDomain() = AppNetworkRule(
        packageName = packageName,
        appName = appName,
        isBlocked = isBlocked,
        blockWifi = blockWifi,
        blockMobileData = blockMobileData,
        blockBackground = blockBackground,
        updatedAt = updatedAt
    )

    private fun AppNetworkRule.toEntity() = AppNetworkRuleEntity(
        packageName = packageName,
        appName = appName,
        isBlocked = isBlocked,
        blockWifi = blockWifi,
        blockMobileData = blockMobileData,
        blockBackground = blockBackground,
        updatedAt = updatedAt
    )

    private fun FirewallLogEntity.toDomain() = FirewallLog(
        id = id,
        timestamp = timestamp,
        packageName = packageName,
        appName = appName,
        action = action,
        networkType = networkType,
        destinationIp = destinationIp,
        destinationPort = destinationPort,
        protocol = protocol
    )

    private fun FirewallLog.toEntity() = FirewallLogEntity(
        id = id,
        timestamp = timestamp,
        packageName = packageName,
        appName = appName,
        action = action,
        networkType = networkType,
        destinationIp = destinationIp,
        destinationPort = destinationPort,
        protocol = protocol
    )
}
