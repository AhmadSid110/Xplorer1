package com.droidexplorer.websim.core.ops

import java.io.Serializable

sealed class OperationProgress : Serializable {
    abstract val operationId: OperationId

    data class Started(
        override val operationId: OperationId,
        val label: String? = null
    ) : OperationProgress()

    data class Running(
        override val operationId: OperationId,
        val current: Long,
        val total: Long?,
        val label: String? = null
    ) : OperationProgress()

    data class Completed(
        override val operationId: OperationId,
        val result: OperationResult
    ) : OperationProgress()
}
