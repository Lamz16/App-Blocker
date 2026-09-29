package lamz.netblocker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import lamz.netblocker.data.model.AppNetworkRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {
    @Query("SELECT * FROM app_network_rules ORDER BY appName ASC")
    fun getAllRulesFlow(): Flow<List<AppNetworkRuleEntity>>

    @Query("SELECT * FROM app_network_rules")
    suspend fun getAllRules(): List<AppNetworkRuleEntity>

    @Query("SELECT * FROM app_network_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun getRuleByPackage(packageName: String): AppNetworkRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRule(rule: AppNetworkRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRules(rules: List<AppNetworkRuleEntity>)

    @Query("DELETE FROM app_network_rules WHERE packageName = :packageName")
    suspend fun deleteRuleByPackage(packageName: String)

    @Query("SELECT COUNT(*) FROM app_network_rules WHERE isBlocked = 1")
    fun getBlockedAppsCountFlow(): Flow<Int>
}
