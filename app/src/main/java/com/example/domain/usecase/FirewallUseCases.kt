package com.example.domain.usecase

import com.example.data.repository.FirewallRepository
import com.example.domain.model.AppFilter
import com.example.domain.model.AppNetworkRule
import com.example.domain.model.FirewallDashboardStats
import com.example.domain.model.FirewallLog
import com.example.domain.model.InstalledApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetInstalledAppsUseCase(private val repository: FirewallRepository) {
    operator fun invoke(
        searchQuery: Flow<String>,
        filter: Flow<AppFilter>
    ): Flow<List<InstalledApp>> {
        return combine(
            repository.getInstalledAppsFlow(),
            searchQuery,
            filter
        ) { apps, query, currentFilter ->
            apps.filter { app ->
                val matchesQuery = query.isBlank() ||
                        app.appName.contains(query, ignoreCase = true) ||
                        app.packageName.contains(query, ignoreCase = true)

                val matchesFilter = when (currentFilter) {
                    AppFilter.ALL -> true
                    AppFilter.BLOCKED -> app.rule.isBlocked || app.rule.blockWifi || app.rule.blockMobileData
                    AppFilter.ALLOWED -> !app.rule.isBlocked && !app.rule.blockWifi && !app.rule.blockMobileData
                    AppFilter.USER_APPS -> !app.isSystemApp
                    AppFilter.SYSTEM_APPS -> app.isSystemApp
                }

                matchesQuery && matchesFilter
            }
        }
    }
}

class ToggleAppBlockUseCase(private val repository: FirewallRepository) {
    suspend fun toggleBlock(packageName: String, isBlocked: Boolean) {
        repository.toggleBlock(packageName, isBlocked)
    }

    suspend fun toggleWifi(packageName: String, blockWifi: Boolean) {
        repository.toggleBlockWifi(packageName, blockWifi)
    }

    suspend fun toggleMobileData(packageName: String, blockMobileData: Boolean) {
        repository.toggleBlockMobileData(packageName, blockMobileData)
    }

    suspend fun toggleBackground(packageName: String, blockBackground: Boolean) {
        repository.toggleBlockBackground(packageName, blockBackground)
    }
}

class GetFirewallStatusUseCase(private val repository: FirewallRepository) {
    operator fun invoke(): Flow<FirewallDashboardStats> {
        return repository.getDashboardStatsFlow()
    }
}

class GetFirewallLogsUseCase(private val repository: FirewallRepository) {
    operator fun invoke(): Flow<List<FirewallLog>> {
        return repository.getLogsFlow()
    }

    suspend fun clearLogs() {
        repository.clearLogs()
    }
}
