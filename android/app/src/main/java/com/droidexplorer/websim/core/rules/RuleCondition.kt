package com.droidexplorer.websim.core.rules

sealed class RuleCondition {
    data class Extension(val extensions: Set<String>) : RuleCondition()
    data class NameMatches(val regex: Regex) : RuleCondition()
    data class MimeType(val types: Set<String>) : RuleCondition()
    data class SizeGreaterThan(val bytes: Long) : RuleCondition()
    data class Composite(
        val all: List<RuleCondition>
    ) : RuleCondition()
}
