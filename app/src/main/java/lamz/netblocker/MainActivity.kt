package lamz.netblocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import lamz.netblocker.ui.activity.ActivityScreen
import lamz.netblocker.ui.activity.ActivityViewModel
import lamz.netblocker.ui.apps.AppsScreen
import lamz.netblocker.ui.apps.AppsViewModel
import lamz.netblocker.ui.dashboard.DashboardScreen
import lamz.netblocker.ui.dashboard.DashboardViewModel
import lamz.netblocker.ui.settings.SettingsScreen
import lamz.netblocker.ui.settings.SettingsViewModel
import lamz.netblocker.ui.theme.NetBlockerTheme

enum class NavScreen(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Dashboard", Icons.Filled.Security, Icons.Outlined.Security),
    APPS("Apps", Icons.Filled.Apps, Icons.Outlined.Apps),
    ACTIVITY("Activity", Icons.Filled.History, Icons.Outlined.History),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as NetBlockerApp
        val factory = app.container.provideViewModelFactory()

        val dashboardViewModel: DashboardViewModel by viewModels { factory }
        val appsViewModel: AppsViewModel by viewModels { factory }
        val activityViewModel: ActivityViewModel by viewModels { factory }
        val settingsViewModel: SettingsViewModel by viewModels { factory }

        setContent {
            NetBlockerTheme {
                MainAppContent(
                    dashboardViewModel = dashboardViewModel,
                    appsViewModel = appsViewModel,
                    activityViewModel = activityViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}

@Composable
fun MainAppContent(
    dashboardViewModel: DashboardViewModel,
    appsViewModel: AppsViewModel,
    activityViewModel: ActivityViewModel,
    settingsViewModel: SettingsViewModel
) {
    var currentScreen by rememberSaveable { mutableStateOf(NavScreen.DASHBOARD) }

    // Handle back button to return to Dashboard if in sub-screen
    BackHandler(enabled = currentScreen != NavScreen.DASHBOARD) {
        currentScreen = NavScreen.DASHBOARD
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    NavScreen.values().forEach { screen ->
                        val selected = currentScreen == screen
                        NavigationRailItem(
                            selected = selected,
                            onClick = { currentScreen = screen },
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_rail_${screen.name.lowercase()}")
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    ScreenContent(
                        currentScreen = currentScreen,
                        dashboardViewModel = dashboardViewModel,
                        appsViewModel = appsViewModel,
                        activityViewModel = activityViewModel,
                        settingsViewModel = settingsViewModel,
                        onNavigateTo = { currentScreen = it }
                    )
                }
            }
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavScreen.values().forEach { screen ->
                            val selected = currentScreen == screen
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentScreen = screen },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = { Text(screen.title) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("bottom_nav_${screen.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    ScreenContent(
                        currentScreen = currentScreen,
                        dashboardViewModel = dashboardViewModel,
                        appsViewModel = appsViewModel,
                        activityViewModel = activityViewModel,
                        settingsViewModel = settingsViewModel,
                        onNavigateTo = { currentScreen = it }
                    )
                }
            }
        }
    }
}

@Composable
fun ScreenContent(
    currentScreen: NavScreen,
    dashboardViewModel: DashboardViewModel,
    appsViewModel: AppsViewModel,
    activityViewModel: ActivityViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateTo: (NavScreen) -> Unit
) {
    when (currentScreen) {
        NavScreen.DASHBOARD -> DashboardScreen(
            viewModel = dashboardViewModel,
            onNavigateToApps = { onNavigateTo(NavScreen.APPS) },
            onNavigateToActivity = { onNavigateTo(NavScreen.ACTIVITY) }
        )
        NavScreen.APPS -> AppsScreen(
            viewModel = appsViewModel
        )
        NavScreen.ACTIVITY -> ActivityScreen(
            viewModel = activityViewModel
        )
        NavScreen.SETTINGS -> SettingsScreen(
            viewModel = settingsViewModel
        )
    }
}
