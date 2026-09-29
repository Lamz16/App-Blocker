package com.example.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.local.FirewallPreferences
import com.example.data.repository.FirewallRepository
import com.example.data.repository.UsageStatsHelper
import com.example.domain.usecase.GetFirewallLogsUseCase
import com.example.domain.usecase.GetFirewallStatusUseCase
import com.example.domain.usecase.GetInstalledAppsUseCase
import com.example.domain.usecase.ToggleAppBlockUseCase
import com.example.firewall.NetworkMonitor
import com.example.ui.activity.ActivityViewModel
import com.example.ui.apps.AppsViewModel
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.settings.SettingsViewModel

class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    val preferences: FirewallPreferences by lazy {
        FirewallPreferences(context)
    }

    val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(context)
    }

    val usageStatsHelper: UsageStatsHelper by lazy {
        UsageStatsHelper(context)
    }

    val repository: FirewallRepository by lazy {
        FirewallRepository(
            context = context,
            appRuleDao = database.appRuleDao(),
            firewallLogDao = database.firewallLogDao(),
            preferences = preferences,
            networkMonitor = networkMonitor,
            usageStatsHelper = usageStatsHelper
        )
    }

    val getInstalledAppsUseCase by lazy {
        GetInstalledAppsUseCase(repository)
    }

    val toggleAppBlockUseCase by lazy {
        ToggleAppBlockUseCase(repository)
    }

    val getFirewallStatusUseCase by lazy {
        GetFirewallStatusUseCase(repository)
    }

    val getFirewallLogsUseCase by lazy {
        GetFirewallLogsUseCase(repository)
    }

    fun provideViewModelFactory(): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when {
                    modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                        DashboardViewModel(repository, getFirewallStatusUseCase) as T
                    }
                    modelClass.isAssignableFrom(AppsViewModel::class.java) -> {
                        AppsViewModel(repository, getInstalledAppsUseCase, toggleAppBlockUseCase) as T
                    }
                    modelClass.isAssignableFrom(ActivityViewModel::class.java) -> {
                        ActivityViewModel(repository, getFirewallLogsUseCase) as T
                    }
                    modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                        SettingsViewModel(repository, usageStatsHelper) as T
                    }
                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
