package com.droidexplorer.websim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.droidexplorer.websim.ui.DualPaneScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DualPaneScreen(singlePane = false)
        }
    }
}
