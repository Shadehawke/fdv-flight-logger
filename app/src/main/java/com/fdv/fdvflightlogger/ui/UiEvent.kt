package com.fdv.fdvflightlogger.ui

import android.net.Uri

sealed interface UiEvent {
    data class ExportSuccess(
        val fileName: String,
        val mimeType: String,
        val uri: Uri
    ) : UiEvent

    data class ExportError(
        val message: String
    ) : UiEvent

    data class Message(val text: String) : UiEvent

}
