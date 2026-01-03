package com.droidexplorer.websim.debug

import android.content.Context
import android.os.StrictMode
import android.util.Log

object StrictModeConfig {
    fun install(context: Context) {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        Log.d("StrictModeConfig", "StrictMode enabled for debug build")
    }
}
