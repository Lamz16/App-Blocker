package lamz.netblocker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "firewall_logs")
data class FirewallLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val appName: String,
    val action: String, // "BLOCKED" or "ALLOWED"
    val networkType: String, // "WIFI", "MOBILE", "UNKNOWN"
    val destinationIp: String? = null,
    val destinationPort: Int? = null,
    val protocol: String? = null // "TCP", "UDP", "ICMP", "OTHER"
)
