package lamz.netblocker.data.repository

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Process
import java.util.Calendar

class UsageStatsHelper(private val context: Context) {

    private val networkStatsManager =
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Returns Pair(rxBytes, txBytes) for a given package UID since the beginning of today.
     */
    fun getAppUsageToday(packageName: String): Pair<Long, Long> {
        if (!hasUsageStatsPermission() || networkStatsManager == null) {
            return Pair(0L, 0L)
        }

        try {
            val packageManager = context.packageManager
            val uid = try {
                packageManager.getApplicationInfo(packageName, 0).uid
            } catch (_: Exception) {
                return Pair(0L, 0L)
            }

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            var rxBytes = 0L
            var txBytes = 0L

            // Wi-Fi traffic
            try {
                val wifiStats = networkStatsManager.queryDetailsForUid(
                    ConnectivityManager.TYPE_WIFI,
                    "",
                    startTime,
                    endTime,
                    uid
                )
                val bucket = NetworkStats.Bucket()
                while (wifiStats.hasNextBucket()) {
                    wifiStats.getNextBucket(bucket)
                    rxBytes += bucket.rxBytes
                    txBytes += bucket.txBytes
                }
                wifiStats.close()
            } catch (_: Exception) {
            }

            // Mobile traffic
            try {
                val mobileStats = networkStatsManager.queryDetailsForUid(
                    ConnectivityManager.TYPE_MOBILE,
                    "",
                    startTime,
                    endTime,
                    uid
                )
                val bucket = NetworkStats.Bucket()
                while (mobileStats.hasNextBucket()) {
                    mobileStats.getNextBucket(bucket)
                    rxBytes += bucket.rxBytes
                    txBytes += bucket.txBytes
                }
                mobileStats.close()
            } catch (_: Exception) {
            }

            return Pair(rxBytes, txBytes)
        } catch (_: Exception) {
            return Pair(0L, 0L)
        }
    }

    /**
     * Returns total Pair(rxBytes, txBytes) for the device today.
     */
    fun getTotalUsageToday(): Pair<Long, Long> {
        if (!hasUsageStatsPermission() || networkStatsManager == null) {
            return Pair(0L, 0L)
        }

        try {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            var rxBytes = 0L
            var txBytes = 0L

            try {
                val wifiBucket = networkStatsManager.querySummaryForDevice(
                    ConnectivityManager.TYPE_WIFI,
                    "",
                    startTime,
                    endTime
                )
                rxBytes += wifiBucket.rxBytes
                txBytes += wifiBucket.txBytes
            } catch (_: Exception) {
            }

            try {
                val mobileBucket = networkStatsManager.querySummaryForDevice(
                    ConnectivityManager.TYPE_MOBILE,
                    "",
                    startTime,
                    endTime
                )
                rxBytes += mobileBucket.rxBytes
                txBytes += mobileBucket.txBytes
            } catch (_: Exception) {
            }

            return Pair(rxBytes, txBytes)
        } catch (_: Exception) {
            return Pair(0L, 0L)
        }
    }
}
