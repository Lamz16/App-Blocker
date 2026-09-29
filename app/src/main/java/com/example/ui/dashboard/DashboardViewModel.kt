package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FirewallRepository
import com.example.domain.model.FirewallDashboardStats
import com.example.domain.model.FirewallLog
import com.example.domain.usecase.GetFirewallStatusUseCase
import com.example.firewall.NetworkVpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val stats: FirewallDashboardStats = FirewallDashboardStats(),
    val isVpnServiceRunning: Boolean = false,
    val recentLogs: List<FirewallLog> = emptyList(),
    val isToggling: Boolean = false
)

class DashboardViewModel(
    private val repository: FirewallRepository,
    private val getFirewallStatusUseCase: GetFirewallStatusUseCase
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        getFirewallStatusUseCase(),
        NetworkVpnService.isServiceRunning,
        repository.getLogsFlow()
    ) { stats, isRunning, logs ->
        DashboardUiState(
            stats = stats.copy(isProtectionActive = isRunning || stats.isProtectionActive),
            isVpnServiceRunning = isRunning,
            recentLogs = logs.take(5)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun setFirewallEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setFirewallEnabled(enabled)
        }
    }
}
