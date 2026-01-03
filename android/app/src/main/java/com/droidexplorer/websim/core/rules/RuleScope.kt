package com.droidexplorer.websim.core.rules

sealed class RuleScope {
    object Downloads : RuleScope()
    object Camera : RuleScope()
    data class Folder(val path: String) : RuleScope()
    object Global : RuleScope()
}
