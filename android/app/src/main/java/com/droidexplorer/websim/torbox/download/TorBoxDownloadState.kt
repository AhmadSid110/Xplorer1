package com.droidexplorer.websim.torbox.download

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

data class TorBoxDownloadState(
    val id: String,
    val name: String,
    val progress: Int,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val status: DownloadStatus,
    val filePath: String?
)
