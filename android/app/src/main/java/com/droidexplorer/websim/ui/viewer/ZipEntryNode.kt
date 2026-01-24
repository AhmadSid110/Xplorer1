package com.droidexplorer.websim.ui.viewer

sealed class ZipEntryNode {
    abstract val name: String
    abstract val path: String
    
    data class File(
        override val name: String,
        override val path: String,
        val size: Long
    ) : ZipEntryNode()
    
    data class Directory(
        override val name: String,
        override val path: String
    ) : ZipEntryNode()
}
