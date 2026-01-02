package com.droidexplorer.websim.storage

import android.net.Uri

interface SafStore {
    fun get(path: String): Uri?
    fun put(path: String, uri: String)
}
