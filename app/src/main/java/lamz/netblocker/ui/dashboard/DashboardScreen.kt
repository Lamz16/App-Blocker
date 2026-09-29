package lamz.netblocker.ui.dashboard

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lamz.netblocker.domain.model.NetworkType
import lamz.netblocker.firewall.NetworkVpnService
import lamz.netblocker.ui.components.AppIconImage
import lamz.netblocker.ui.theme.StatusAllowed
import lamz.netblocker.ui.theme.StatusBlocked
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToApps: () -> Unit,
    onNavigateToActivity: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            NetworkVpnService.startService(context)
            viewModel.setFirewallEnabled(true)
        }
    }

    val isProtectionActive = uiState.isVpnServiceRunning || uiState.stats.isProtectionActive

    fun onToggleProtection(enabled: Boolean) {
        if (enabled) {
            val prepareIntent = VpnService.prepare(context)
            if (prepareIntent != null) {
                vpnLauncher.launch(prepareIntent)
            } else {
                NetworkVpnService.startService(context)
                viewModel.setFirewallEnabled(true)
            }
        } else {
            NetworkVpnService.stopService(context)
            viewModel.setFirewallEnabled(false)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            DashboardHeader()
        }

        item {
            HeroMasterSwitchCard(
                isActive = isProtectionActive,
                blockedCount = uiState.stats.blockedAppsCount,
                onToggle = { onToggleProtection(it) }
            )
        }

        item {
            StatsGrid(
                blockedAppsCount = uiState.stats.blockedAppsCount,
                allowedAppsCount = uiState.stats.allowedAppsCount,
                activeNetworkType = uiState.stats.activeNetworkType,
                bytesSavedEstimate = uiState.stats.totalBytesSavedEstimate
            )
        }

        item {
            QuickActionsSection(
                onNavigateToApps = onNavigateToApps,
                onNavigateToActivity = onNavigateToActivity
            )
        }

        item {
            RecentActivitySection(
                recentLogs = uiState.recentLogs,
                onViewAllClick = onNavigateToActivity
            )
        }

        item {
            PrivacyAssuranceCard()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DashboardHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "NetBlocker",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Per-App Network Firewall",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(20.dp)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Local VPN",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun HeroMasterSwitchCard(
    isActive: Boolean,
    blockedCount: Int,
    onToggle: (Boolean) -> Unit
) {
    val activeGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            if (isActive) Color(0xFF00382E) else MaterialTheme.colorScheme.surfaceVariant
        )
    )

    val shieldTint by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary else Color(0xFF64748B),
        label = "shieldColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_master_switch_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = if (isActive) Brush.linearGradient(
                listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), Color.Transparent)
            ) else Brush.linearGradient(listOf(Color(0xFF2A3859), Color.Transparent))
        )
    ) {
        Box(
            modifier = Modifier
                .background(activeGradient)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else Color(0xFF1E293B)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = shieldTint,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (isActive) "Protection Active" else "Protection Paused",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isActive) "$blockedCount apps blocked on active network"
                            else "All apps have normal network access",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isActive,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("master_firewall_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun StatsGrid(
    blockedAppsCount: Int,
    allowedAppsCount: Int,
    activeNetworkType: NetworkType,
    bytesSavedEstimate: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Blocked Apps",
            value = blockedAppsCount.toString(),
            icon = Icons.Default.Block,
            iconTint = StatusBlocked
        )

        StatCard(
            modifier = Modifier.weight(1f),
            title = "Allowed Apps",
            value = allowedAppsCount.toString(),
            icon = Icons.Default.CheckCircle,
            iconTint = StatusAllowed
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val (netIcon, netName) = when (activeNetworkType) {
            NetworkType.WIFI -> Pair(Icons.Default.Wifi, "Wi-Fi")
            NetworkType.MOBILE -> Pair(Icons.Default.NetworkCell, "Mobile")
            NetworkType.DISCONNECTED -> Pair(Icons.Default.WifiOff, "Offline")
            NetworkType.OTHER -> Pair(Icons.Default.Wifi, "Active")
        }

        StatCard(
            modifier = Modifier.weight(1f),
            title = "Active Network",
            value = netName,
            icon = netIcon,
            iconTint = MaterialTheme.colorScheme.secondary
        )

        val savedStr = if (bytesSavedEstimate >= 1024 * 1024 * 1024) {
            String.format(Locale.US, "%.1f GB", bytesSavedEstimate / (1024f * 1024f * 1024f))
        } else {
            String.format(Locale.US, "%d MB", bytesSavedEstimate / (1024 * 1024))
        }

        StatCard(
            modifier = Modifier.weight(1f),
            title = "Data Saved Est.",
            value = savedStr,
            icon = Icons.Default.Security,
            iconTint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun QuickActionsSection(
    onNavigateToApps: () -> Unit,
    onNavigateToActivity: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onNavigateToApps,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("manage_apps_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "Manage Apps",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }

        Button(
            onClick = onNavigateToActivity,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("view_activity_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = "Activity Log",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RecentActivitySection(
    recentLogs: List<lamz.netblocker.domain.model.FirewallLog>,
    onViewAllClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.clickable(onClick = onViewAllClick)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (recentLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No connections blocked yet. All active apps allowed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentLogs.forEach { log ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                AppIconImage(packageName = log.packageName, size = 32.dp)
                                Column {
                                    Text(
                                        text = log.appName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${timeFormat.format(Date(log.timestamp))} • ${log.destinationIp ?: "Packet drop"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }

                            val isBlocked = log.action == "BLOCKED"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isBlocked) StatusBlocked.copy(alpha = 0.15f) else StatusAllowed.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = log.action,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isBlocked) StatusBlocked else StatusAllowed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyAssuranceCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "100% On-Device & Private",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Firewall operates locally using Android VpnService. No data is sent to external servers or remote VPNs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
