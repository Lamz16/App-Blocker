package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FirewallRepository
import com.example.data.repository.UsageStatsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val showSystemApps: Boolean = false,
    val blockByDefault: Boolean = false,
    val blockBackgroundGlobal: Boolean = false,
    val hasUsageStatsPermission: Boolean = false
)

class SettingsViewModel(
    private val repository: FirewallRepository,
    private val usageStatsHelper: UsageStatsHelper
) : ViewModel() {

    private val _hasUsagePermission = MutableStateFlow(usageStatsHelper.hasUsageStatsPermission())
    val hasUsagePermission = _hasUsagePermission.asStateFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.showSystemAppsFlow,
        repository.blockByDefaultFlow,
        repository.blockBackgroundGlobalFlow,
        _hasUsagePermission
    ) { showSystem, blockDefault, blockBg, hasPerm ->
        SettingsUiState(
            showSystemApps = showSystem,
            blockByDefault = blockDefault,
            blockBackgroundGlobal = blockBg,
            hasUsageStatsPermission = hasPerm
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun refreshUsagePermission() {
        _hasUsagePermission.value = usageStatsHelper.hasUsageStatsPermission()
    }

    fun setShowSystemApps(show: Boolean) {
        viewModelScope.launch {
            repository.setShowSystemApps(show)
        }
    }

    fun setBlockByDefault(block: Boolean) {
        viewModelScope.launch {
            repository.setBlockByDefault(block)
        }
    }

    fun setBlockBackgroundGlobal(block: Boolean) {
        viewModelScope.launch {
            repository.setBlockBackgroundGlobal(block)
        }
    }
}
