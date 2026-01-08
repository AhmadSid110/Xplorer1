package com.droidexplorer.websim.storage

import android.os.Build
import android.os.Environment

/**
 * Checks if the app has full file access permission (MANAGE_EXTERNAL_STORAGE).
 * 
 * On Android 11+ (API 30+), this permission allows unrestricted access to all files
 * on external storage without the scoped storage restrictions.
 * 
 * On Android 10 and below, this always returns true as scoped storage is not enforced.
 * 
 * @return true if the app has full file access, false if running in limited/scoped mode
 */
fun hasAllFilesAccess(): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
            Environment.isExternalStorageManager()
}
