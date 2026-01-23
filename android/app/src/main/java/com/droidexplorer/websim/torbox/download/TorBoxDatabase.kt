package com.droidexplorer.websim.torbox.download

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [TorBoxDownloadEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(TorBoxDownloadConverters::class)
abstract class TorBoxDatabase : RoomDatabase() {
    abstract fun dao(): TorBoxDownloadDao
}
