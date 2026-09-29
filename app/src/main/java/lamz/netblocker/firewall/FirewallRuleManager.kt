package lamz.netblocker.firewall

import lamz.netblocker.domain.model.AppNetworkRule
import lamz.netblocker.domain.model.NetworkType

class FirewallRuleManager {

    /**
     * Evaluates whether a given package should be blocked on the current network.
     */
    fun evaluateRule(
        rule: AppNetworkRule,
        firewallEnabled: Boolean,
        networkType: NetworkType,
        isScreenOff: Boolean = false,
        isBackground: Boolean = false
    ): Boolean {
        if (!firewallEnabled) return false

        // 1. General master block for this app
        if (rule.isBlocked) return true

        // 2. Wi-Fi specific block
        if (networkType == NetworkType.WIFI && rule.blockWifi) {
            return true
        }

        // 3. Mobile data specific block
        if (networkType == NetworkType.MOBILE && rule.blockMobileData) {
            return true
        }

        // 4. Background network block (triggered when screen is off or app is in background)
        if (rule.blockBackground && (isScreenOff || isBackground)) {
            return true
        }

        return false
    }

    /**
     * Computes the set of package names that should have their network traffic blocked.
     */
    fun computeBlockedPackages(
        rules: List<AppNetworkRule>,
        firewallEnabled: Boolean,
        networkType: NetworkType,
        isScreenOff: Boolean = false
    ): Set<String> {
        if (!firewallEnabled) return emptySet()

        return rules
            .filter { rule ->
                evaluateRule(
                    rule = rule,
                    firewallEnabled = firewallEnabled,
                    networkType = networkType,
                    isScreenOff = isScreenOff,
                    isBackground = false
                )
            }
            .map { it.packageName }
            .toSet()
    }
}
