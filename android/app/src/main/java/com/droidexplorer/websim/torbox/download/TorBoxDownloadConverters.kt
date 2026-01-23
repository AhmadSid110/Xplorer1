package com.droidexplorer.websim.torbox.download

import androidx.room.TypeConverter

class TorBoxDownloadConverters {
    @TypeConverter
    fun fromStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): DownloadStatus = DownloadStatus.valueOf(value)
}
