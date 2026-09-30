package lamz.netblocker.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import lamz.netblocker.data.local.AppDatabase
import lamz.netblocker.data.local.FirewallPreferences
import lamz.netblocker.data.model.FirewallLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * A UI-level guard for browser URLs exposed through Android accessibility.
 * It deliberately does not inspect encrypted network traffic.
 */
class WebsiteBlockAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var blockedDomains: Set<String> = emptySet()
    @Volatile private var blockedAppPackages: Set<String> = emptySet()
    private var lastBlockedValue: String? = null
    private var lastBlockedAt = 0L
    private lateinit var database: AppDatabase

    override fun onServiceConnected() {
        super.onServiceConnected()
        database = AppDatabase.getInstance(applicationContext)
        scope.launch {
            FirewallPreferences(applicationContext).customDomainBlocklistFlow.collectLatest {
                blockedDomains = it
            }
        }
        scope.launch {
            FirewallPreferences(applicationContext).appLaunchBlocklistFlow.collectLatest {
                blockedAppPackages = it
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType !in watchedEvents) return
        val browserPackage = event.packageName?.toString() ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && browserPackage in blockedAppPackages) {
            blockAppOpening(browserPackage)
            return
        }
        if (blockedDomains.isEmpty()) return
        // Never inspect this app's own Settings screen. The custom blocklist is
        // visible there, so scanning every package created false positives.
        if (browserPackage !in supportedBrowserPackages) return
        val visibleText = buildString {
            event.text.forEach { append(it).append(' ') }
            event.contentDescription?.let { append(it).append(' ') }
            event.source?.appendVisibleText(this, MAX_NODE_TEXT)
        }
        val domain = domainPattern.find(visibleText)?.groupValues?.getOrNull(1)?.lowercase()?.trimEnd('.') ?: return
        if (blockedDomains.none { domain == it || domain.endsWith(".$it") }) return
        block(domain, browserPackage)
    }

    override fun onInterrupt() = Unit

    private fun blockAppOpening(packageName: String) {
        val now = System.currentTimeMillis()
        // Enforcement must never be debounced: every launch attempt needs to
        // return to Home. Only the user feedback and database log are limited.
        performGlobalAction(GLOBAL_ACTION_HOME)
        if (packageName == lastBlockedValue && now - lastBlockedAt < BLOCK_DEBOUNCE_MS) return
        lastBlockedValue = packageName
        lastBlockedAt = now
        mainHandler.post {
            Toast.makeText(this, "App blocked", Toast.LENGTH_SHORT).show()
        }
        scope.launch {
            database.firewallLogDao().insertLog(
                FirewallLogEntity(
                    packageName = packageName,
                    appName = packageName,
                    action = "BLOCKED_APP_OPENING",
                    networkType = "UI",
                    protocol = "ACCESSIBILITY"
                )
            )
        }
    }

    private fun block(domain: String, browserPackage: String) {
        val now = System.currentTimeMillis()
        if (domain == lastBlockedValue && now - lastBlockedAt < BLOCK_DEBOUNCE_MS) return
        lastBlockedValue = domain
        lastBlockedAt = now
        performGlobalAction(GLOBAL_ACTION_HOME)
        mainHandler.post {
            Toast.makeText(this, "Website blocked: $domain", Toast.LENGTH_SHORT).show()
        }
        scope.launch {
            database.firewallLogDao().insertLog(
                FirewallLogEntity(
                    packageName = browserPackage,
                    appName = "Website Block Guard",
                    action = "BLOCKED_CUSTOM_DOMAIN",
                    networkType = "UI",
                    destinationIp = domain,
                    protocol = "ACCESSIBILITY"
                )
            )
        }
    }

    private fun AccessibilityNodeInfo.appendVisibleText(builder: StringBuilder, limit: Int) {
        if (builder.length >= limit) return
        text?.let { builder.append(it).append(' ') }
        contentDescription?.let { builder.append(it).append(' ') }
        for (index in 0 until childCount) getChild(index)?.appendVisibleText(builder, limit)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        val watchedEvents = setOf(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        )
        val supportedBrowserPackages = setOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.chrome.canary",
            "org.mozilla.firefox",
            "org.mozilla.firefox_beta",
            "com.microsoft.emmx",
            "com.sec.android.app.sbrowser",
            "com.mi.globalbrowser",
            "com.opera.browser",
            "com.brave.browser"
        )
        val domainPattern = Regex("(?:https?://)?([a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+)", RegexOption.IGNORE_CASE)
        const val MAX_NODE_TEXT = 4_096
        const val BLOCK_DEBOUNCE_MS = 2_000L
    }
}
