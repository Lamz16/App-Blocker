package lamz.netblocker.ui.apps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lamz.netblocker.domain.model.AppFilter
import lamz.netblocker.domain.model.InstalledApp
import lamz.netblocker.ui.components.AppIconImage
import lamz.netblocker.ui.theme.StatusAllowed
import lamz.netblocker.ui.theme.StatusBlocked
import java.util.Locale

@Composable
fun AppsScreen(
    viewModel: AppsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Search Bar
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.onQueryChange(it) },
            placeholder = { Text("Search installed applications...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_search_field"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipItem("All", filter == AppFilter.ALL) { viewModel.onFilterSelect(AppFilter.ALL) }
            FilterChipItem("Blocked (${uiState.blockedAppsCount})", filter == AppFilter.BLOCKED) { viewModel.onFilterSelect(AppFilter.BLOCKED) }
            FilterChipItem("Allowed (${uiState.totalAppsCount - uiState.blockedAppsCount})", filter == AppFilter.ALLOWED) { viewModel.onFilterSelect(AppFilter.ALLOWED) }
            FilterChipItem("User Apps", filter == AppFilter.USER_APPS) { viewModel.onFilterSelect(AppFilter.USER_APPS) }
            FilterChipItem("System Apps", filter == AppFilter.SYSTEM_APPS) { viewModel.onFilterSelect(AppFilter.SYSTEM_APPS) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (uiState.apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No applications match your filter",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try adjusting your search query or filter selection",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = uiState.apps,
                    key = { it.packageName }
                ) { app ->
                    AppItemCard(
                        app = app,
                        onToggleMaster = { viewModel.toggleBlock(app.packageName, it) },
                        onToggleWifi = { viewModel.toggleWifi(app.packageName, it) },
                        onToggleMobile = { viewModel.toggleMobileData(app.packageName, it) },
                        onToggleBackground = { viewModel.toggleBackground(app.packageName, it) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun AppItemCard(
    app: InstalledApp,
    onToggleMaster: (Boolean) -> Unit,
    onToggleWifi: (Boolean) -> Unit,
    onToggleMobile: (Boolean) -> Unit,
    onToggleBackground: (Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val isEffectivelyBlocked = app.rule.isBlocked || app.rule.blockWifi || app.rule.blockMobileData

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
            .testTag("app_card_${app.packageName}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEffectivelyBlocked)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    AppIconImage(packageName = app.packageName, size = 44.dp)
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            if (app.isSystemApp) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "SYS",
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = if (app.rule.isBlocked) "Internet: Blocked"
                            else if (app.rule.blockWifi && app.rule.blockMobileData) "Wi-Fi & Mobile: Blocked"
                            else if (app.rule.blockWifi) "Wi-Fi: Blocked"
                            else if (app.rule.blockMobileData) "Mobile Data: Blocked"
                            else "Internet: Allowed",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isEffectivelyBlocked) StatusBlocked else StatusAllowed
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Switch(
                        checked = app.rule.isBlocked,
                        onCheckedChange = onToggleMaster,
                        modifier = Modifier.testTag("app_toggle_${app.packageName}"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StatusBlocked,
                            checkedTrackColor = StatusBlocked.copy(alpha = 0.3f),
                            uncheckedThumbColor = StatusAllowed,
                            uncheckedTrackColor = StatusAllowed.copy(alpha = 0.3f)
                        )
                    )

                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Text(
                        text = "Granular Network Controls",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Wi-Fi Rule Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Block on Wi-Fi",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = app.rule.blockWifi,
                            onCheckedChange = onToggleWifi,
                            modifier = Modifier.testTag("wifi_toggle_${app.packageName}")
                        )
                    }

                    // Mobile Data Rule Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NetworkCell,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Block on Mobile Data",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = app.rule.blockMobileData,
                            onCheckedChange = onToggleMobile,
                            modifier = Modifier.testTag("mobile_toggle_${app.packageName}")
                        )
                    }

                    // Background Internet Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Block Background Traffic",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = app.rule.blockBackground,
                            onCheckedChange = onToggleBackground,
                            modifier = Modifier.testTag("background_toggle_${app.packageName}")
                        )
                    }

                    if (app.totalBytes > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val rxStr = formatBytes(app.downloadBytes)
                        val txStr = formatBytes(app.uploadBytes)
                        val totalStr = formatBytes(app.totalBytes)
                        Text(
                            text = "Today's Usage: ↓ $rxStr • ↑ $txStr (Total: $totalStr)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f GB", bytes / (1024f * 1024f * 1024f))
        bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
        bytes >= 1024 -> String.format(Locale.US, "%d KB", bytes / 1024)
        else -> "$bytes B"
    }
}
