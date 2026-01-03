package com.droidexplorer.websim.ui.selection

class SelectionController<T>(
    private val idSelector: (T) -> String
) {
    private val selectedIds = LinkedHashSet<String>()
    private var anchorId: String? = null

    fun isSelected(item: T): Boolean = selectedIds.contains(idSelector(item))

    fun selected(): Set<String> = selectedIds.toSet()

    fun clear() {
        selectedIds.clear()
        anchorId = null
    }

    fun select(item: T) {
        val id = idSelector(item)
        selectedIds.add(id)
        anchorId = id
    }

    fun deselect(item: T) {
        selectedIds.remove(idSelector(item))
    }

    fun toggle(item: T) {
        val id = idSelector(item)
        if (!selectedIds.add(id)) {
            selectedIds.remove(id)
        } else {
            anchorId = id
        }
    }

    /**
     * Select a contiguous range between the last anchor and [target].
     * If no anchor exists, the target becomes the anchor.
     */
    fun selectRange(items: List<T>, target: T) {
        if (items.isEmpty()) return
        val targetId = idSelector(target)
        val anchor = anchorId ?: targetId
        val start = items.indexOfFirst { idSelector(it) == anchor }
        val end = items.indexOfFirst { idSelector(it) == targetId }
        if (start == -1 || end == -1) {
            select(target)
            return
        }
        val range = if (start <= end) start..end else end..start
        range.forEach { index ->
            selectedIds.add(idSelector(items[index]))
        }
        anchorId = anchor
    }

    /**
     * Invert the current selection relative to the provided [items] list.
     */
    fun invertSelection(items: List<T>) {
        val current = selectedIds.toSet()
        selectedIds.clear()
        items.forEach { item ->
            val id = idSelector(item)
            if (id !in current) {
                selectedIds.add(id)
            }
        }
        anchorId = selectedIds.lastOrNull()
    }

    /**
     * Select all entries whose extension matches [extension], ignoring case.
     * [extensionOf] should return the extension for each item.
     */
    fun selectByExtension(
        items: List<T>,
        extension: String,
        extensionOf: (T) -> String
    ): List<T> {
        val normalized = normalizeExtension(extension)
        if (normalized.isEmpty()) return emptyList()

        val newlySelected = items.filter { extensionOf(it).lowercase() == normalized }
        newlySelected.forEach { item ->
            selectedIds.add(idSelector(item))
        }
        anchorId = newlySelected.lastOrNull()?.let { idSelector(it) }
        return newlySelected
    }

    fun currentSelection(items: List<T>): List<T> =
        items.filter { selectedIds.contains(idSelector(it)) }

    private fun normalizeExtension(extension: String): String =
        extension.trim().trimStart('.').lowercase()
}
