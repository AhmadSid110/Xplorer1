package com.droidexplorer.websim.torbox.download

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object TorBoxDatabaseProvider {
    @Volatile
    private var instance: TorBoxDatabase? = null

    fun get(context: Context): TorBoxDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                TorBoxDatabase::class.java,
                "torbox_downloads.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }
    }

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE torbox_downloads ADD COLUMN speedBytesPerSec INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE torbox_downloads ADD COLUMN sourceUrl TEXT"
            )
        }
    }
}
