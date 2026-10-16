package com.stillfeed.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.stillfeed.app.data.local.dao.ActiveUnlockSessionDao
import com.stillfeed.app.data.local.dao.AwarenessMonthlyDao
import com.stillfeed.app.data.local.dao.DeviceBindingDao
import com.stillfeed.app.data.local.dao.ProtectionStateDao
import com.stillfeed.app.data.local.dao.UnlockHistoryDao
import com.stillfeed.app.data.local.dao.UsageEventDao
import com.stillfeed.app.data.local.dao.UserDao
import com.stillfeed.app.data.local.entity.ActiveUnlockSessionEntity
import com.stillfeed.app.data.local.entity.AwarenessMonthlyEntity
import com.stillfeed.app.data.local.entity.DeviceBindingEntity
import com.stillfeed.app.data.local.entity.ProtectionStateEntity
import com.stillfeed.app.data.local.entity.UnlockEventEntity
import com.stillfeed.app.data.local.entity.UsageEventEntity
import com.stillfeed.app.data.local.entity.UserEntity

/**
 * Main Room database for STILLFEED local persistence.
 *
 * Design Guarantees:
 * - Versioned schema starting at version 1 (upgraded to version 2 in STEP 08, version 3 in STEP 19).
 * - Migration-ready: Does NOT use destructive fallback in production.
 * - Enforces relational integrity and query indexes.
 */
@Database(
    entities = [
        UserEntity::class,
        ProtectionStateEntity::class,
        UsageEventEntity::class,
        AwarenessMonthlyEntity::class,
        UnlockEventEntity::class,
        DeviceBindingEntity::class,
        ActiveUnlockSessionEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class StillfeedDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun protectionStateDao(): ProtectionStateDao
    abstract fun usageEventDao(): UsageEventDao
    abstract fun awarenessMonthlyDao(): AwarenessMonthlyDao
    abstract fun unlockHistoryDao(): UnlockHistoryDao
    abstract fun deviceBindingDao(): DeviceBindingDao
    abstract fun activeUnlockSessionDao(): ActiveUnlockSessionDao

    companion object {
        const val DATABASE_NAME = "stillfeed.db"
        const val CURRENT_SCHEMA_VERSION = 3

        /**
         * Migration from version 1 to 2: adds target, reduction amount, and status columns
         * to the awareness_monthly_state table without data loss.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE awareness_monthly_state ADD COLUMN targetUsageMinutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE awareness_monthly_state ADD COLUMN reductionAmountMinutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE awareness_monthly_state ADD COLUMN status TEXT NOT NULL DEFAULT 'BASELINE_FORMING'")
            }
        }

        /**
         * Migration from version 2 to 3 (STEP 19): adds active_unlock_sessions table
         * for durable batch unlock session persistence.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `active_unlock_sessions` (
                        `id` TEXT NOT NULL,
                        `package_name` TEXT NOT NULL,
                        `selected_batch_allowance` INTEGER NOT NULL,
                        `remaining_batch_allowance` INTEGER NOT NULL,
                        `activity_type` TEXT NOT NULL,
                        `verified_activity_completion` INTEGER NOT NULL,
                        `created_at_millis` INTEGER NOT NULL,
                        `expires_at_millis` INTEGER NOT NULL,
                        `is_indeterminate_tracking` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_active_unlock_sessions_package_name` ON `active_unlock_sessions` (`package_name`)"
                )
            }
        }

        /**
         * Registry of future migrations to ensure production schema upgrades without data loss.
         */
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)

        @Volatile
        private var INSTANCE: StillfeedDatabase? = null

        /**
         * Returns the production singleton instance of [StillfeedDatabase].
         */
        fun getInstance(context: Context): StillfeedDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): StillfeedDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                StillfeedDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(*MIGRATIONS)
                // Note: Destructive migration is explicitly omitted to protect local user data.
                .build()
        }

        /**
         * Creates an isolated in-memory database strictly for automated unit tests.
         */
        fun buildInMemory(
            context: Context,
            executor: java.util.concurrent.Executor? = null
        ): StillfeedDatabase {
            val builder = Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                StillfeedDatabase::class.java
            ).allowMainThreadQueries()

            if (executor != null) {
                builder.setQueryExecutor(executor)
                builder.setTransactionExecutor(executor)
            }

            return builder.build()
        }
    }
}
