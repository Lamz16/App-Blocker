package lamz.netblocker

import lamz.netblocker.domain.model.AppNetworkRule
import lamz.netblocker.domain.model.NetworkType
import lamz.netblocker.firewall.FirewallRuleManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FirewallRuleManagerTest {

    private lateinit var ruleManager: FirewallRuleManager

    @Before
    fun setUp() {
        ruleManager = FirewallRuleManager()
    }

    @Test
    fun `when firewall disabled, all apps are allowed`() {
        val rule = AppNetworkRule(
            packageName = "com.instagram.android",
            appName = "Instagram",
            isBlocked = true,
            blockWifi = true,
            blockMobileData = true
        )

        val shouldBlockWifi = ruleManager.evaluateRule(
            rule = rule,
            firewallEnabled = false,
            networkType = NetworkType.WIFI
        )
        val shouldBlockMobile = ruleManager.evaluateRule(
            rule = rule,
            firewallEnabled = false,
            networkType = NetworkType.MOBILE
        )

        assertFalse(shouldBlockWifi)
        assertFalse(shouldBlockMobile)
    }

    @Test
    fun `when app blocked unconditionally, blocked on all networks`() {
        val rule = AppNetworkRule(
            packageName = "com.tiktok.android",
            appName = "TikTok",
            isBlocked = true,
            blockWifi = false,
            blockMobileData = false
        )

        val blockedOnWifi = ruleManager.evaluateRule(
            rule = rule,
            firewallEnabled = true,
            networkType = NetworkType.WIFI
        )
        val blockedOnMobile = ruleManager.evaluateRule(
            rule = rule,
            firewallEnabled = true,
            networkType = NetworkType.MOBILE
        )

        assertTrue(blockedOnWifi)
        assertTrue(blockedOnMobile)
    }

    @Test
    fun `when app blocked on wifi only, allowed on mobile data`() {
        val rule = AppNetworkRule(
            packageName = "com.youtube.android",
            appName = "YouTube",
            isBlocked = false,
            blockWifi = true,
            blockMobileData = false
        )

        val blockedOnWifi = ruleManager.evaluateRule(
            rule = rule,
            firewallEnabled = true,
            networkType = NetworkType.WIFI
        )
        val blockedOnMobile = ruleManager.evaluateRule(
            rule = rule,
            firewallEnabled = true,
            networkType = NetworkType.MOBILE
        )

        assertTrue(blockedOnWifi)
        assertFalse(blockedOnMobile)
    }

    @Test
    fun `when app blocked on mobile data only, allowed on wifi`() {
        val rule = AppNetworkRule(
            packageName = "com.netflix.mediaclient",
            appName = "Netflix",
            isBlocked = false,
            blockWifi = false,
            blockMobileData = true
        )

        val blockedOnWifi = ruleManager.evaluateRule(
            rule = rule,
            firewallEnabled = true,
            networkType = NetworkType.WIFI
        )
        val blockedOnMobile = ruleManager.evaluateRule(
            rule = rule,
            firewallEnabled = true,
            networkType = NetworkType.MOBILE
        )

        assertFalse(blockedOnWifi)
        assertTrue(blockedOnMobile)
    }

    @Test
    fun `when app allowed on all, not blocked`() {
        val rule = AppNetworkRule(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            isBlocked = false,
            blockWifi = false,
            blockMobileData = false
        )

        val blockedOnWifi = ruleManager.evaluateRule(
            rule = rule,
            firewallEnabled = true,
            networkType = NetworkType.WIFI
        )
        val blockedOnMobile = ruleManager.evaluateRule(
            rule = rule,
            firewallEnabled = true,
            networkType = NetworkType.MOBILE
        )

        assertFalse(blockedOnWifi)
        assertFalse(blockedOnMobile)
    }

    @Test
    fun `when background blocked, blocks when screen is off`() {
        val rule = AppNetworkRule(
            packageName = "com.facebook.katana",
            appName = "Facebook",
            isBlocked = false,
            blockWifi = false,
            blockMobileData = false,
            blockBackground = true
        )

        val blockedScreenOn = ruleManager.evaluateRule(
            rule = rule,
            firewallEnabled = true,
            networkType = NetworkType.WIFI,
            isScreenOff = false
        )
        val blockedScreenOff = ruleManager.evaluateRule(
            rule = rule,
            firewallEnabled = true,
            networkType = NetworkType.WIFI,
            isScreenOff = true
        )

        assertFalse(blockedScreenOn)
        assertTrue(blockedScreenOff)
    }

    @Test
    fun `computeBlockedPackages returns correct set of package names`() {
        val rules = listOf(
            AppNetworkRule("com.app.a", "App A", isBlocked = true),
            AppNetworkRule("com.app.b", "App B", isBlocked = false, blockWifi = true),
            AppNetworkRule("com.app.c", "App C", isBlocked = false, blockMobileData = true),
            AppNetworkRule("com.app.d", "App D", isBlocked = false)
        )

        // Wi-Fi network: A and B should be blocked
        val wifiBlocked = ruleManager.computeBlockedPackages(rules, firewallEnabled = true, NetworkType.WIFI)
        assertEquals(setOf("com.app.a", "com.app.b"), wifiBlocked)

        // Mobile network: A and C should be blocked
        val mobileBlocked = ruleManager.computeBlockedPackages(rules, firewallEnabled = true, NetworkType.MOBILE)
        assertEquals(setOf("com.app.a", "com.app.c"), mobileBlocked)

        // Firewall disabled: none should be blocked
        val disabledBlocked = ruleManager.computeBlockedPackages(rules, firewallEnabled = false, NetworkType.WIFI)
        assertTrue(disabledBlocked.isEmpty())
    }
}
