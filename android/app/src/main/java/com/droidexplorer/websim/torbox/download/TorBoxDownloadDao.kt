package com.droidexplorer.websim.torbox.download

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TorBoxDownloadDao {

    @Query("SELECT * FROM torbox_downloads")
    fun observeAll(): Flow<List<TorBoxDownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TorBoxDownloadEntity)

    @Query("SELECT * FROM torbox_downloads WHERE id = :id")
    suspend fun get(id: String): TorBoxDownloadEntity?
}
