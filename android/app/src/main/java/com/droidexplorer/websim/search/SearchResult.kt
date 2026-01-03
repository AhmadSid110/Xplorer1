package com.droidexplorer.websim.search

import com.droidexplorer.websim.file.FsNode

data class SearchResult(
    val matches: List<FsNode>,
    val skippedRoots: List<SearchRoot>
)
