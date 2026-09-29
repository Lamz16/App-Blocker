package lamz.netblocker.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import lamz.netblocker.data.local.AppDatabase
import lamz.netblocker.data.local.FirewallPreferences
import lamz.netblocker.data.repository.FirewallRepository
import lamz.netblocker.data.repository.UsageStatsHelper
import lamz.netblocker.domain.usecase.GetFirewallLogsUseCase
import lamz.netblocker.domain.usecase.GetFirewallStatusUseCase
import lamz.netblocker.domain.usecase.GetInstalledAppsUseCase
import lamz.netblocker.domain.usecase.ToggleAppBlockUseCase
import lamz.netblocker.firewall.NetworkMonitor
import lamz.netblocker.ui.activity.ActivityViewModel
import lamz.netblocker.ui.apps.AppsViewModel
import lamz.netblocker.ui.dashboard.DashboardViewModel
import lamz.netblocker.ui.settings.SettingsViewModel

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
