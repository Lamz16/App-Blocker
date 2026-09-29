package lamz.netblocker.firewall

/** Local, suffix-based DNS policy. Entries are intentionally conservative. */
object DomainBlocklist {
    private val advertising = setOf(
        "doubleclick.net", "googleadservices.com", "googlesyndication.com",
        "adservice.google.com", "adnxs.com", "adsrvr.org", "taboola.com",
        "outbrain.com", "criteo.com", "scorecardresearch.com"
    )
    private val tracking = setOf(
        "google-analytics.com", "analytics.google.com", "mixpanel.com",
        "segment.io", "segment.com", "hotjar.com", "app-measurement.com"
    )
    private val maliciousRedirect = setOf(
        "popads.net", "popcash.net", "propellerads.com", "pushwoosh.com"
    )

    fun categoryFor(domain: String): String? {
        val normalized = domain.trim().trimEnd('.').lowercase()
        return when {
            advertising.any { normalized == it || normalized.endsWith(".$it") } -> "AD"
            tracking.any { normalized == it || normalized.endsWith(".$it") } -> "TRACKER"
            maliciousRedirect.any { normalized == it || normalized.endsWith(".$it") } -> "REDIRECT"
            else -> null
        }
    }
}
