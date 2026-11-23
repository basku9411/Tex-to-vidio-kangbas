package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [VideoHistoryEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoHistoryDao(): VideoHistoryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `video_history_new` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL DEFAULT '',
                        `title` TEXT NOT NULL,
                        `modelLabel` TEXT NOT NULL,
                        `mode` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `progressText` TEXT NOT NULL,
                        `thumbPath` TEXT,
                        `taskId` TEXT,
                        `videoUrl` TEXT,
                        `error` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `completedAt` INTEGER,
                        `estimatedCredits` INTEGER NOT NULL,
                        `draftJson` TEXT,
                        PRIMARY KEY(`userId`, `id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO `video_history_new` (
                        `id`, `userId`, `title`, `modelLabel`, `mode`, `status`,
                        `progressText`, `thumbPath`, `taskId`, `videoUrl`, `error`,
                        `createdAt`, `completedAt`, `estimatedCredits`, `draftJson`
                    )
                    SELECT
                        `id`, '', `title`, `modelLabel`, `mode`, `status`,
                        `progressText`, `thumbPath`, `taskId`, `videoUrl`, `error`,
                        `createdAt`, `completedAt`, `estimatedCredits`, NULL
                    FROM `video_history`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `video_history`")
                db.execSQL("ALTER TABLE `video_history_new` RENAME TO `video_history`")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kangbas_video.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
