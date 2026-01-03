package com.droidexplorer.websim.core.rules

import android.content.Context
import com.droidexplorer.websim.core.ops.FileOperation
import com.droidexplorer.websim.core.ops.NodeRef
import com.droidexplorer.websim.file.FsNode
import com.droidexplorer.websim.file.asFile
import com.droidexplorer.websim.service.FileOperationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RuleExecutor(
    private val context: Context,
    private val historyStore: RuleHistoryStore = RuleHistoryStore(context)
) {

    data class Plan(
        val operations: List<FileOperation>,
        val rollback: List<FileOperation>,
        val matchedRuleIds: List<String>
    )

    fun plan(file: FsNode, matches: List<RuleMatch>): Plan {
        val operations = mutableListOf<FileOperation>()
        val rollback = mutableListOf<FileOperation>()
        matches.forEach { match ->
            match.actions.forEach { action ->
                val op = toOperation(file, action)
                if (op != null) {
                    operations.add(op)
                    rollback.addIfNotNull(rollbackFor(file, action, op))
                }
            }
        }
        return Plan(operations, rollback, matches.map { it.rule.id })
    }

    suspend fun execute(
        file: FsNode,
        matches: List<RuleMatch>,
        preview: Boolean = false
    ): Plan {
        val plan = plan(file, matches)
        if (!preview) {
            withContext(Dispatchers.IO) {
                plan.operations.forEach { op ->
                    FileOperationService.enqueue(context, op)
                }
                if (plan.operations.isNotEmpty()) {
                    historyStore.record(
                        RuleHistoryEntry(
                            ruleId = plan.matchedRuleIds.joinToString(","),
                            affectedFiles = listOf(file.path),
                            operations = plan.operations,
                            rollbackOperations = plan.rollback,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
        return plan
    }

    suspend fun undoLatest(): Boolean {
        val latest = historyStore.popLatest() ?: return false
        latest.rollbackOperations.forEach { op ->
            FileOperationService.enqueue(context, op)
        }
        return true
    }

    private fun toOperation(file: FsNode, action: RuleAction): FileOperation? {
        val backing = file.asFile()
        return when (action) {
            is RuleAction.Move -> {
                val destDir = resolveDestination(backing, action.destination)
                FileOperation.Move(NodeRef.from(backing), NodeRef.from(destDir))
            }

            is RuleAction.Copy -> {
                val destDir = resolveDestination(backing, action.destination)
                FileOperation.Copy(NodeRef.from(backing), NodeRef.from(destDir))
            }

            is RuleAction.Rename -> {
                val newName = action.pattern.applyTo(backing)
                FileOperation.Rename(NodeRef.from(backing), newName)
            }

            is RuleAction.Tag -> null
        }
    }

    private fun rollbackFor(
        file: FsNode,
        action: RuleAction,
        operation: FileOperation
    ): FileOperation? {
        return when (action) {
            is RuleAction.Move -> {
                val moveOp = operation as? FileOperation.Move ?: return null
                val originalSource = moveOp.source.asFile()
                val movedFile = File(moveOp.destinationDir.asFile(), originalSource.name)
                val originalParent = originalSource.parentFile ?: return null
                FileOperation.Move(NodeRef.from(movedFile), NodeRef.from(originalParent))
            }

            is RuleAction.Copy -> {
                val copyOp = operation as? FileOperation.Copy ?: return null
                val originalSource = copyOp.source.asFile()
                val copiedFile = File(copyOp.destinationDir.asFile(), originalSource.name)
                FileOperation.Delete(NodeRef.from(copiedFile))
            }

            is RuleAction.Rename -> {
                val renameOp = operation as? FileOperation.Rename ?: return null
                val renamedFile = File(renameOp.target.asFile().parentFile, renameOp.newName)
                FileOperation.Rename(NodeRef.from(renamedFile), renameOp.target.asFile().name)
            }

            is RuleAction.Tag -> null
        }
    }

    private fun resolveDestination(file: File, destination: RuleDestination): File {
        return when (destination) {
            RuleDestination.SameFolder -> file.parentFile ?: file
            is RuleDestination.Folder -> File(destination.path)
            is RuleDestination.ByDate -> {
                val formatter = SimpleDateFormat(destination.pattern.ifBlank { "yyyy/MM" }, Locale.US)
                val base = file.parentFile ?: file
                File(base, formatter.format(Date(file.lastModified().takeIf { it > 0 } ?: System.currentTimeMillis())))
            }

            RuleDestination.SameAsVideo -> {
                val parent = file.parentFile ?: return file.parentFile ?: file
                val base = file.nameWithoutExtension
                val sibling = parent.listFiles()?.firstOrNull {
                    val ext = it.extension.lowercase(Locale.getDefault())
                    (ext == "mp4" || ext == "mkv" || ext == "mov") && it.nameWithoutExtension == base
                }
                sibling?.parentFile ?: parent
            }
        }
    }

    private fun MutableList<FileOperation>.addIfNotNull(op: FileOperation?) {
        if (op != null) add(op)
    }
}
