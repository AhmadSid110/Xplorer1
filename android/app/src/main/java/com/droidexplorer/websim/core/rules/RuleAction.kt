package com.droidexplorer.websim.core.rules

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class RuleAction {
    data class Move(val destination: RuleDestination) : RuleAction()
    data class Rename(val pattern: RenamePattern) : RuleAction()
    data class Copy(val destination: RuleDestination) : RuleAction()
    data class Tag(val tag: String) : RuleAction()
}

sealed class RuleDestination {
    object SameFolder : RuleDestination()
    object SameAsVideo : RuleDestination()
    data class ByDate(val pattern: String) : RuleDestination()
    data class Folder(val path: String) : RuleDestination()
}

data class RenamePattern(
    val pattern: String
) {
    fun applyTo(file: File, counter: Int = 1): String {
        val name = file.nameWithoutExtension
        val ext = file.extension.takeIf { it.isNotBlank() }?.let { ".$it" } ?: ""
        val date = Date(file.lastModified().takeIf { it > 0 } ?: System.currentTimeMillis())
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return pattern
            .replace("{name}", name)
            .replace("{ext}", ext.removePrefix("."))
            .replace("{counter}", counter.toString().padStart(2, '0'))
            .replace("{yyyy-MM-dd}", formatter.format(date))
            .trim()
    }
}
