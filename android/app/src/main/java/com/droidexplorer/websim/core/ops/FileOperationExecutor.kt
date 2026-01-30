package com.droidexplorer.websim.core.ops

import com.droidexplorer.websim.file.FileOperator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class FileOperationExecutor(
    private val fileOperator: FileOperator
) {

    fun execute(
        operation: FileOperation,
        cancellationToken: OperationCancellationToken = OperationCancellationToken()
    ): Flow<OperationProgress> = channelFlow {
        if (cancellationToken.isCancelled) {
            trySend(
                OperationProgress.Completed(
                    operation.id,
                    OperationResult.Cancelled
                )
            )
            close()
            return@channelFlow
        }

        trySend(OperationProgress.Started(operation.id, describe(operation)))

        val result = withContext(Dispatchers.IO) {
            perform(operation, cancellationToken) { progress ->
                trySend(progress)
            }
        }

        trySend(OperationProgress.Completed(operation.id, result))
    }.flowOn(Dispatchers.IO)

    private suspend fun perform(
        operation: FileOperation,
        cancellationToken: OperationCancellationToken,
        emit: suspend (OperationProgress) -> Unit
    ): OperationResult {
        if (cancellationToken.isCancelled) return OperationResult.Cancelled

        return when (operation) {
            is FileOperation.Copy -> performCopy(operation, cancellationToken, emit)
            is FileOperation.Move -> performMove(operation, cancellationToken, emit)
            is FileOperation.Delete -> performDelete(operation, cancellationToken, emit)
            is FileOperation.Rename -> performRename(operation, cancellationToken, emit)
            is FileOperation.CreateFile -> performCreateFile(operation, cancellationToken, emit)
            is FileOperation.CreateDirectory -> performCreateDirectory(operation, cancellationToken, emit)
        }
    }

    private suspend fun performCopy(
        operation: FileOperation.Copy,
        cancellationToken: OperationCancellationToken,
        emit: suspend (OperationProgress) -> Unit
    ): OperationResult {
        val source = operation.source.asFile()
        val destDir = operation.destinationDir.asFile()
        val total = estimateSize(source)
        emit(OperationProgress.Running(operation.id, 0, total, "Copying ${source.name}"))
        if (cancellationToken.isCancelled) return OperationResult.Cancelled

        val result = fileOperator.copy(source, destDir)
        if (cancellationToken.isCancelled) return OperationResult.Cancelled

        return result.fold(
            onSuccess = { copied ->
                emit(
                    OperationProgress.Running(
                        operation.id,
                        total,
                        total,
                        "Copied ${source.name}"
                    )
                )
                OperationResult.Success(NodeRef.from(copied))
            },
            onFailure = { OperationResult.Failure(it.message ?: "Copy failed") }
        )
    }

    private suspend fun performMove(
        operation: FileOperation.Move,
        cancellationToken: OperationCancellationToken,
        emit: suspend (OperationProgress) -> Unit
    ): OperationResult {
        val source = operation.source.asFile()
        val destDir = operation.destinationDir.asFile()
        val total = estimateSize(source)
        emit(OperationProgress.Running(operation.id, 0, total, "Moving ${source.name}"))
        if (cancellationToken.isCancelled) return OperationResult.Cancelled

        val result = fileOperator.move(source, destDir)
        if (cancellationToken.isCancelled) return OperationResult.Cancelled

        return result.fold(
            onSuccess = { moved ->
                emit(
                    OperationProgress.Running(
                        operation.id,
                        total,
                        total,
                        "Moved ${source.name}"
                    )
                )
                OperationResult.Success(NodeRef.from(moved))
            },
            onFailure = { OperationResult.Failure(it.message ?: "Move failed") }
        )
    }

    private suspend fun performDelete(
        operation: FileOperation.Delete,
        cancellationToken: OperationCancellationToken,
        emit: suspend (OperationProgress) -> Unit
    ): OperationResult {
        val target = operation.target.asFile()
        val total = estimateSize(target)
        emit(OperationProgress.Running(operation.id, 0, total, "Deleting ${target.name}"))
        if (cancellationToken.isCancelled) return OperationResult.Cancelled

        val result = fileOperator.delete(target)
        if (cancellationToken.isCancelled) return OperationResult.Cancelled

        return result.fold(
            onSuccess = {
                emit(OperationProgress.Running(operation.id, total, total, "Deleted ${target.name}"))
                OperationResult.Success()
            },
            onFailure = { OperationResult.Failure(it.message ?: "Delete failed") }
        )
    }

    private suspend fun performRename(
        operation: FileOperation.Rename,
        cancellationToken: OperationCancellationToken,
        emit: suspend (OperationProgress) -> Unit
    ): OperationResult {
        val target = operation.target.asFile()
        emit(OperationProgress.Running(operation.id, 0, null, "Renaming ${target.name}"))
        if (cancellationToken.isCancelled) return OperationResult.Cancelled

        val result = fileOperator.rename(target, operation.newName)
        if (cancellationToken.isCancelled) return OperationResult.Cancelled

        return result.fold(
            onSuccess = { renamed ->
                emit(OperationProgress.Running(operation.id, 1, 1, "Renamed to ${renamed.name}"))
                OperationResult.Success(NodeRef.from(renamed))
            },
            onFailure = { OperationResult.Failure(it.message ?: "Rename failed") }
        )
    }

    private suspend fun performCreateFile(
        operation: FileOperation.CreateFile,
        cancellationToken: OperationCancellationToken,
        emit: suspend (OperationProgress) -> Unit
    ): OperationResult {
        val parent = operation.parentDir.asFile()
        emit(OperationProgress.Running(operation.id, 0, null, "Creating file ${operation.name}"))
        val result = fileOperator.createFile(parent, operation.name)
        return result.fold(
            onSuccess = { file ->
                emit(OperationProgress.Running(operation.id, 1, 1, "File created"))
                OperationResult.Success(NodeRef.from(file))
            },
            onFailure = { OperationResult.Failure(it.message ?: "Failed to create file") }
        )
    }

    private suspend fun performCreateDirectory(
        operation: FileOperation.CreateDirectory,
        cancellationToken: OperationCancellationToken,
        emit: suspend (OperationProgress) -> Unit
    ): OperationResult {
        val parent = operation.parentDir.asFile()
        emit(OperationProgress.Running(operation.id, 0, null, "Creating folder ${operation.name}"))
        val result = fileOperator.createDirectory(parent, operation.name)
        return result.fold(
            onSuccess = { dir ->
                emit(OperationProgress.Running(operation.id, 1, 1, "Folder created"))
                OperationResult.Success(NodeRef.from(dir))
            },
            onFailure = { OperationResult.Failure(it.message ?: "Failed to create folder") }
        )
    }

    private fun describe(operation: FileOperation): String = when (operation) {
        is FileOperation.Copy -> "Copy ${operation.source.path}"
        is FileOperation.Move -> "Move ${operation.source.path}"
        is FileOperation.Delete -> "Delete ${operation.target.path}"
        is FileOperation.Rename -> "Rename ${operation.target.path}"
        is FileOperation.CreateFile -> "Create file ${operation.name}"
        is FileOperation.CreateDirectory -> "Create folder ${operation.name}"
    }

    private fun estimateSize(file: File): Long {
        return try {
            if (file.isDirectory) {
                file.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            } else {
                file.length()
            }
        } catch (_: Exception) {
            0L
        }
    }
}
