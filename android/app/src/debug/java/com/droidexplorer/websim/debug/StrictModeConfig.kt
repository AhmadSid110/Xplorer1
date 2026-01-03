package com.droidexplorer.websim.debug

import android.os.StrictMode

object StrictModeConfig {
    fun install() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .detectFileUriExposure()
                .penaltyLog()
                .build()
        )
    }
}
