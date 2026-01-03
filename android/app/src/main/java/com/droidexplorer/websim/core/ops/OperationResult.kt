package com.droidexplorer.websim.core.ops

import java.io.Serializable

sealed class OperationResult : Serializable {
    data class Success(val output: NodeRef? = null, val message: String? = null) : OperationResult()
    data class Failure(val message: String) : OperationResult()
    object Cancelled : OperationResult()
}
