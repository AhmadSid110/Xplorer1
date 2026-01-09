package com.droidexplorer.websim.ui.events

import android.net.Uri

sealed interface UiEvent {
    data class RequestSafAccess(
        val initialUri: Uri?
    ) : UiEvent
    
    data object RequestAllFilesAccess : UiEvent
    
    data object ShowTorBoxSetup : UiEvent
}
