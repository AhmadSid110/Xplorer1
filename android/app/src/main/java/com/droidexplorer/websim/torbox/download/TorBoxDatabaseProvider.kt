package com.droidexplorer.websim.torbox.download

import android.content.Context
import androidx.room.Room

object TorBoxDatabaseProvider {
    @Volatile
    private var instance: TorBoxDatabase? = null

    fun get(context: Context): TorBoxDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                TorBoxDatabase::class.java,
                "torbox_downloads.db"
            ).build().also { instance = it }
        }
    }
}
