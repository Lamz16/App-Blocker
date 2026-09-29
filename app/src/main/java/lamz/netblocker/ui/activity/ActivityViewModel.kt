package lamz.netblocker.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import lamz.netblocker.data.repository.FirewallRepository
import lamz.netblocker.domain.model.FirewallLog
import lamz.netblocker.domain.usecase.GetFirewallLogsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActivityUiState(
    val logs: List<FirewallLog> = emptyList(),
    val filterAction: String? = null, // null for ALL, "BLOCKED", "ALLOWED"
    val isLoading: Boolean = false
)

class ActivityViewModel(
    private val repository: FirewallRepository,
    private val getFirewallLogsUseCase: GetFirewallLogsUseCase
) : ViewModel() {

    private val _filterAction = MutableStateFlow<String?>(null)
    val filterAction = _filterAction.asStateFlow()

    val uiState: StateFlow<ActivityUiState> = combine(
        getFirewallLogsUseCase(),
        _filterAction
    ) { logs, filter ->
        val filteredLogs = if (filter == null) {
            logs
        } else {
            logs.filter { log ->
                when (filter) {
                    "BLOCKED" -> log.action.startsWith("BLOCKED", ignoreCase = true)
                    "ALLOWED" -> log.action.startsWith("ALLOWED", ignoreCase = true)
                    else -> log.action.equals(filter, ignoreCase = true)
                }
            }
        }
        ActivityUiState(
            logs = filteredLogs,
            filterAction = filter,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActivityUiState(isLoading = true)
    )

    fun setFilterAction(filter: String?) {
        _filterAction.value = filter
    }

    fun clearLogs() {
        viewModelScope.launch {
            getFirewallLogsUseCase.clearLogs()
        }
    }
}
