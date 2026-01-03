package com.droidexplorer.websim.core.rules

import com.droidexplorer.websim.file.FsNode
import com.droidexplorer.websim.file.asFile
import java.io.File

data class RuleMatch(
    val rule: FileRule,
    val actions: List<RuleAction> = listOf(rule.action)
)

class RuleEngine {
    fun evaluate(
        file: FsNode,
        rules: List<FileRule>
    ): List<RuleMatch> {
        return rules.filter { it.enabled }
            .filter { scopeMatches(file, it.scope) && conditionMatches(file, it.condition) }
            .map { RuleMatch(it) }
    }

    private fun scopeMatches(file: FsNode, scope: RuleScope): Boolean {
        val path = file.path.lowercase()
        return when (scope) {
            RuleScope.Global -> true
            RuleScope.Downloads -> path.contains("/download")
            RuleScope.Camera -> path.contains("/dcim") || path.contains("/camera")
            is RuleScope.Folder -> path.startsWith(scope.path.lowercase())
        }
    }

    private fun conditionMatches(file: FsNode, condition: RuleCondition): Boolean {
        val backingFile: File = file.asFile()
        return when (condition) {
            is RuleCondition.Extension -> condition.extensions.any { ext ->
                backingFile.extension.equals(ext, ignoreCase = true)
            }

            is RuleCondition.NameMatches -> condition.regex.containsMatchIn(backingFile.name)
            is RuleCondition.MimeType -> {
                val mime = runCatching {
                    java.nio.file.Files.probeContentType(backingFile.toPath())
                }.getOrNull() ?: ""
                condition.types.any { mime.equals(it, ignoreCase = true) }
            }

            is RuleCondition.SizeGreaterThan -> backingFile.length() > condition.bytes
            is RuleCondition.Composite -> condition.all.all { conditionMatches(file, it) }
        }
    }
}
