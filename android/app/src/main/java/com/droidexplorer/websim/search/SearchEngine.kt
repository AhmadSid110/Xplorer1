package com.droidexplorer.websim.search

import com.droidexplorer.websim.file.FsNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class SearchEngine(
    private val fileSearcher: FileSearcher
) {

    fun search(
        query: String,
        roots: List<SearchRoot>
    ): Flow<SearchResult> = flow {

        val matches = mutableListOf<FsNode>()
        val skipped = mutableListOf<SearchRoot>()

        roots.forEach { root ->
            when (root) {
                is SearchRoot.Local -> {
                    val result = fileSearcher.searchLocal(
                        path = root.path,
                        query = query
                    )
                    matches += result
                }

                is SearchRoot.Saf -> {
                    val result = fileSearcher.searchSaf(
                        rootId = root.id,
                        query = query
                    )
                    if (result == null) {
                        skipped += root
                    } else {
                        matches += result
                    }
                }
            }
        }

        emit(
            SearchResult(
                matches = matches,
                skippedRoots = skipped
            )
        )
    }.flowOn(Dispatchers.IO)
}
