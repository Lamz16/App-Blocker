package lamz.netblocker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import lamz.netblocker.data.model.AppNetworkRuleEntity
import lamz.netblocker.data.model.FirewallLogEntity

@Database(
    entities = [
        AppNetworkRuleEntity::class,
        FirewallLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appRuleDao(): AppRuleDao
    abstract fun firewallLogDao(): FirewallLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "netblocker_database"
                )
                    .fallbackToDestructiveMigration(false)
                .build().also { INSTANCE = it }
            }
        }
    }
}
