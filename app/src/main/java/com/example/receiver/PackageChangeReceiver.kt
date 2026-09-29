package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.data.local.AppDatabase
import com.example.data.local.FirewallPreferences
import com.example.data.model.AppNetworkRuleEntity
import com.example.firewall.NetworkVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PackageChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                val db = AppDatabase.getInstance(context)
                val preferences = FirewallPreferences(context)

                when (intent.action) {
                    Intent.ACTION_PACKAGE_REMOVED -> {
                        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                        if (!isReplacing) {
                            db.appRuleDao().deleteRuleByPackage(packageName)
                            if (NetworkVpnService.isServiceRunning.value) {
                                NetworkVpnService.updateRules(context)
                            }
                        }
                    }
                    Intent.ACTION_PACKAGE_ADDED -> {
                        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                        if (!isReplacing) {
                            val blockByDefault = preferences.blockByDefaultFlow.first()
                            val appName = try {
                                val pm = context.packageManager
                                val info = pm.getApplicationInfo(packageName, 0)
                                pm.getApplicationLabel(info).toString()
                            } catch (_: Exception) {
                                packageName
                            }

                            if (blockByDefault) {
                                db.appRuleDao().upsertRule(
                                    AppNetworkRuleEntity(
                                        packageName = packageName,
                                        appName = appName,
                                        isBlocked = true
                                    )
                                )
                                if (NetworkVpnService.isServiceRunning.value) {
                                    NetworkVpnService.updateRules(context)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
