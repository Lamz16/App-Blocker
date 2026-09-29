package lamz.netblocker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import lamz.netblocker.data.repository.FirewallRepository
import lamz.netblocker.data.repository.UsageStatsHelper
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
    val adBlockingEnabled: Boolean = false,
    val hasUsageStatsPermission: Boolean = false
)

class SettingsViewModel(
    private val repository: FirewallRepository,
    private val usageStatsHelper: UsageStatsHelper
) : ViewModel() {

    private val _hasUsagePermission = MutableStateFlow(usageStatsHelper.hasUsageStatsPermission())
    val hasUsagePermission = _hasUsagePermission.asStateFlow()
    private val _hasVpnPermission = MutableStateFlow(repository.hasVpnPermission())
    val hasVpnPermission = _hasVpnPermission.asStateFlow()
    private val _isWebsiteBlockGuardEnabled = MutableStateFlow(repository.isWebsiteBlockGuardEnabled())
    val isWebsiteBlockGuardEnabled = _isWebsiteBlockGuardEnabled.asStateFlow()

    val customBlockedDomains: StateFlow<Set<String>> = repository.customDomainBlocklistFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.showSystemAppsFlow,
        repository.blockByDefaultFlow,
        repository.blockBackgroundGlobalFlow,
        repository.adBlockingEnabledFlow,
        _hasUsagePermission
    ) { showSystem, blockDefault, blockBg, adBlocking, hasPerm ->
        SettingsUiState(
            showSystemApps = showSystem,
            blockByDefault = blockDefault,
            blockBackgroundGlobal = blockBg,
            adBlockingEnabled = adBlocking,
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

    fun refreshSystemPermissionStates() {
        refreshUsagePermission()
        _hasVpnPermission.value = repository.hasVpnPermission()
        _isWebsiteBlockGuardEnabled.value = repository.isWebsiteBlockGuardEnabled()
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

    fun setAdBlockingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAdBlockingEnabled(enabled)
        }
    }

    fun addCustomBlockedDomain(domain: String) {
        viewModelScope.launch {
            runCatching { repository.addCustomBlockedDomain(domain) }
        }
    }

    fun removeCustomBlockedDomain(domain: String) {
        viewModelScope.launch {
            repository.removeCustomBlockedDomain(domain)
        }
    }
}
