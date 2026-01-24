package com.droidexplorer.websim.file

data class ZipEntryItem(
    val name: String,
    val size: Long,
    val isDirectory: Boolean
)
