package com.droidexplorer.websim.search

sealed class SearchRoot {
    data class Local(val path: String) : SearchRoot()
    data class Saf(val id: String) : SearchRoot()
}
