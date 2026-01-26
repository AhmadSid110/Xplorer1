package com.droidexplorer.websim.ui.viewer

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset

@Stable
class ZoomState(
    scale: Float = 1f,
    offset: Offset = Offset.Zero,
    rotation: Float = 0f
) {
    var scale by mutableStateOf(scale)
    var offset by mutableStateOf(offset)
    var rotation by mutableStateOf(rotation)

    fun reset() {
        scale = 1f
        offset = Offset.Zero
        rotation = 0f
    }
}
