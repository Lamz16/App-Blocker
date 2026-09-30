package lamz.netblocker.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import lamz.netblocker.data.repository.FirewallRepository
import lamz.netblocker.domain.model.AppFilter
import lamz.netblocker.domain.model.InstalledApp
import lamz.netblocker.domain.usecase.GetInstalledAppsUseCase
import lamz.netblocker.domain.usecase.ToggleAppBlockUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppsUiState(
    val isLoading: Boolean = false,
    val apps: List<InstalledApp> = emptyList(),
    val query: String = "",
    val filter: AppFilter = AppFilter.ALL,
    val totalAppsCount: Int = 0,
    val blockedAppsCount: Int = 0,
    val error: String? = null
)

class AppsViewModel(
    private val repository: FirewallRepository,
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val toggleAppBlockUseCase: ToggleAppBlockUseCase
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _filter = MutableStateFlow(AppFilter.ALL)
    val filter = _filter.asStateFlow()

    private val _isAccessibilityGuardEnabled = MutableStateFlow(repository.isWebsiteBlockGuardEnabled())
    val isAccessibilityGuardEnabled = _isAccessibilityGuardEnabled.asStateFlow()

    val uiState: StateFlow<AppsUiState> = combine(
        getInstalledAppsUseCase(_query, _filter),
        repository.getInstalledAppsFlow(),
        _query,
        _filter
    ) { filteredApps, allApps, currentQuery, currentFilter ->
        AppsUiState(
            isLoading = false,
            apps = filteredApps,
            query = currentQuery,
            filter = currentFilter,
            totalAppsCount = allApps.size,
            blockedAppsCount = allApps.count { it.isLaunchBlocked || it.rule.isBlocked || it.rule.blockWifi || it.rule.blockMobileData }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppsUiState(isLoading = true)
    )

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun onFilterSelect(newFilter: AppFilter) {
        _filter.value = newFilter
    }

    fun toggleBlock(packageName: String, isBlocked: Boolean) {
        viewModelScope.launch {
            toggleAppBlockUseCase.toggleBlock(packageName, isBlocked)
        }
    }

    fun toggleWifi(packageName: String, blockWifi: Boolean) {
        viewModelScope.launch {
            toggleAppBlockUseCase.toggleWifi(packageName, blockWifi)
        }
    }

    fun toggleMobileData(packageName: String, blockMobileData: Boolean) {
        viewModelScope.launch {
            toggleAppBlockUseCase.toggleMobileData(packageName, blockMobileData)
        }
    }

    fun toggleBackground(packageName: String, blockBackground: Boolean) {
        viewModelScope.launch {
            toggleAppBlockUseCase.toggleBackground(packageName, blockBackground)
        }
    }

    fun toggleAppOpening(packageName: String, blocked: Boolean) {
        viewModelScope.launch {
            repository.setAppLaunchBlocked(packageName, blocked)
        }
    }

    fun refreshAccessibilityGuardState() {
        _isAccessibilityGuardEnabled.value = repository.isWebsiteBlockGuardEnabled()
    }
}
