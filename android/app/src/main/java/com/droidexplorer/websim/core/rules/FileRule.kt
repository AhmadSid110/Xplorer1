package com.droidexplorer.websim.core.rules

data class FileRule(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val scope: RuleScope,
    val condition: RuleCondition,
    val action: RuleAction
)
