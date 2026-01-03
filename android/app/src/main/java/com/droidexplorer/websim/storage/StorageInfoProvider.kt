package com.droidexplorer.websim.storage

import android.os.StatFs

data class StorageInfo(
    val total: Long,
    val free: Long,
    val used: Long
)

class StorageInfoProvider {

    fun internalStorage(): StorageInfo {
        val stat = StatFs("/storage/emulated/0")
        val total = stat.totalBytes
        val free = stat.availableBytes
        val used = total - free

        return StorageInfo(
            total = total,
            free = free,
            used = used
        )
    }
}
