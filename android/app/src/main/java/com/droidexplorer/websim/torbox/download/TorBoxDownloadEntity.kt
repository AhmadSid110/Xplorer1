package com.droidexplorer.websim.torbox.download

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "torbox_downloads")
data class TorBoxDownloadEntity(
    @PrimaryKey val id: String,
    val name: String,
    val downloaded: Long,
    val total: Long,
    val status: DownloadStatus,
    val path: String?,
    val speedBytesPerSec: Long,
    val sourceUrl: String?
)
