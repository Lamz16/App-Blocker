package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "firewall_settings")

class FirewallPreferences(private val context: Context) {

    private object PreferencesKeys {
        val FIREWALL_ENABLED = booleanPreferencesKey("firewall_enabled")
        val SHOW_SYSTEM_APPS = booleanPreferencesKey("show_system_apps")
        val BLOCK_BY_DEFAULT = booleanPreferencesKey("block_by_default")
        val BLOCK_BACKGROUND_GLOBAL = booleanPreferencesKey("block_background_global")
        val AUTO_START_ON_BOOT = booleanPreferencesKey("auto_start_on_boot")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    val isFirewallEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.FIREWALL_ENABLED] ?: false
    }

    val showSystemAppsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_SYSTEM_APPS] ?: false
    }

    val blockByDefaultFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BLOCK_BY_DEFAULT] ?: false
    }

    val blockBackgroundGlobalFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BLOCK_BACKGROUND_GLOBAL] ?: false
    }

    val autoStartOnBootFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUTO_START_ON_BOOT] ?: true
    }

    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setFirewallEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIREWALL_ENABLED] = enabled
        }
    }

    suspend fun setShowSystemApps(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_SYSTEM_APPS] = show
        }
    }

    suspend fun setBlockByDefault(block: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BLOCK_BY_DEFAULT] = block
        }
    }

    suspend fun setBlockBackgroundGlobal(block: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BLOCK_BACKGROUND_GLOBAL] = block
        }
    }

    suspend fun setAutoStartOnBoot(autoStart: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_START_ON_BOOT] = autoStart
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }
}
