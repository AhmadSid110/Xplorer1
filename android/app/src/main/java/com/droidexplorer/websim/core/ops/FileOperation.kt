package com.droidexplorer.websim.core.ops

import com.droidexplorer.websim.file.FsNode
import com.droidexplorer.websim.file.asFile
import java.io.File
import java.io.Serializable
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lightweight representation of a filesystem node that avoids Android framework classes.
 */
data class NodeRef(
    val path: String,
    val isDirectory: Boolean
) : Serializable {
    fun asFile(): File = File(path)

    companion object {
        fun from(node: FsNode): NodeRef = NodeRef(node.path, node.isDirectory)
        fun from(file: File): NodeRef = NodeRef(file.absolutePath, file.isDirectory)
    }
}

data class OperationId(val value: String = UUID.randomUUID().toString()) : Serializable

class OperationCancellationToken : Serializable {
    @Transient
    private val cancelled = AtomicBoolean(false)

    fun cancel() {
        cancelled.set(true)
    }

    val isCancelled: Boolean
        get() = cancelled.get()
}

sealed class FileOperation(open val id: OperationId = OperationId()) : Serializable {
    data class Copy(
        val source: NodeRef,
        val destinationDir: NodeRef,
        override val id: OperationId = OperationId()
    ) : FileOperation(id)

    data class Move(
        val source: NodeRef,
        val destinationDir: NodeRef,
        override val id: OperationId = OperationId()
    ) : FileOperation(id)

    data class Delete(
        val target: NodeRef,
        override val id: OperationId = OperationId()
    ) : FileOperation(id)

    data class Rename(
        val target: NodeRef,
        val newName: String,
        override val id: OperationId = OperationId()
    ) : FileOperation(id)

    data class CreateFile(
        val parentDir: NodeRef,
        val name: String,
        override val id: OperationId = OperationId()
    ) : FileOperation(id)

    data class CreateDirectory(
        val parentDir: NodeRef,
        val name: String,
        override val id: OperationId = OperationId()
    ) : FileOperation(id)

    companion object {
        fun copy(source: FsNode, destinationDir: File): Copy =
            Copy(NodeRef.from(source), NodeRef.from(destinationDir))

        fun move(source: FsNode, destinationDir: File): Move =
            Move(NodeRef.from(source), NodeRef.from(destinationDir))

        fun delete(target: FsNode): Delete = Delete(NodeRef.from(target))

        fun delete(target: File): Delete = Delete(NodeRef.from(target))

        fun rename(target: FsNode, newName: String): Rename = Rename(NodeRef.from(target), newName)
    }
}
