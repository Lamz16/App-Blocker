package lamz.netblocker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import lamz.netblocker.data.model.FirewallLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FirewallLogDao {
    @Query("SELECT * FROM firewall_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogsFlow(limit: Int = 200): Flow<List<FirewallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: FirewallLogEntity)

    @Query("DELETE FROM firewall_logs")
    suspend fun clearAllLogs()

    @Query("SELECT COUNT(*) FROM firewall_logs WHERE action = 'BLOCKED'")
    fun getBlockedEventsCountFlow(): Flow<Int>
}
