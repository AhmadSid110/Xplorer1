package com.droidexplorer.websim.ui

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.setValue

enum class PaneMode { SINGLE, DUAL }

class PaneNavigator(
    startPath: String,
    backStack: List<String> = emptyList(),
    forwardStack: List<String> = emptyList()
) {

    var currentPath: String = startPath
        private set

    private val backStack = ArrayDeque(backStack)
    private val forwardStack = ArrayDeque(forwardStack)

    fun canGoBack(): Boolean = backStack.isNotEmpty()
    fun canGoForward(): Boolean = forwardStack.isNotEmpty()

    fun navigateTo(newPath: String) {
        // TorBox paths are always normalized to root (no folder navigation)
        val normalizedPath = if (newPath.startsWith("torbox:")) "torbox:" else newPath
        
        if (normalizedPath == currentPath) return
        backStack.addLast(currentPath)
        forwardStack.clear()
        currentPath = normalizedPath
    }

    fun goBack() {
        if (backStack.isNotEmpty()) {
            forwardStack.addLast(currentPath)
            currentPath = backStack.removeLast()
        }
    }

    fun goForward() {
        if (forwardStack.isNotEmpty()) {
            backStack.addLast(currentPath)
            currentPath = forwardStack.removeLast()
        }
    }

    fun navigateToPath(path: String) {
        if (path != currentPath) {
            navigateTo(path)
        }
    }

    internal fun backStackSnapshot(): List<String> = backStack.toList()
    internal fun forwardStackSnapshot(): List<String> = forwardStack.toList()
}

class PaneState private constructor(
    private val defaultPath: String,
    initialTabs: List<TabState>,
    initialActiveIndex: Int
) {
    private val _tabs = mutableStateListOf<TabState>().apply {
        addAll(initialTabs.ifEmpty { listOf(TabState.create(defaultPath)) })
    }
    val tabs: List<TabState> get() = _tabs

    var activeTabIndex by mutableIntStateOf(initialActiveIndex.coerceIn(0, _tabs.lastIndex))
        private set
    var path: String by mutableStateOf(_tabs.getOrNull(activeTabIndex)?.path ?: defaultPath)
        private set

    val activeTab: TabState?
        get() = _tabs.getOrNull(activeTabIndex)

    fun canGoBack() = activeTab?.canGoBack() == true
    fun canGoForward() = activeTab?.canGoForward() == true

    fun navigateTo(newPath: String) {
        activeTab?.navigateTo(newPath)
        syncPath()
    }

    fun navigateToPath(newPath: String) {
        activeTab?.navigateToPath(newPath)
        syncPath()
    }

    fun goBack() {
        activeTab?.goBack()
        syncPath()
    }

    fun goForward() {
        activeTab?.goForward()
        syncPath()
    }

    fun addTab(path: String = activeTab?.path ?: defaultPath) {
        _tabs.add(TabState.create(path))
        activeTabIndex = _tabs.lastIndex
        syncPath()
    }

    fun closeTab(index: Int) {
        if (_tabs.size <= 1 || index !in _tabs.indices) return
        _tabs.removeAt(index)
        activeTabIndex = activeTabIndex.coerceAtMost(_tabs.lastIndex)
        syncPath()
    }

    fun selectTab(index: Int) {
        if (index !in _tabs.indices) return
        activeTabIndex = index
        syncPath()
    }

    private fun syncPath() {
        path = activeTab?.path ?: defaultPath
    }

    fun snapshot(): Map<String, Any> = mapOf(
        "default" to defaultPath,
        "active" to activeTabIndex,
        "tabs" to tabs.map { it.toSaverMap() }
    )

    companion object {
        fun saver(defaultPath: String): Saver<PaneState, Any> = mapSaver(
            save = { state -> state.snapshot() },
            restore = { restored ->
                val basePath = restored["default"] as? String ?: defaultPath
                val rawTabs = restored["tabs"] as? List<*>
                val restoredTabs = rawTabs?.mapNotNull { TabState.fromSaved(it) }.orEmpty()
                if (rawTabs?.isNotEmpty() == true && restoredTabs.isEmpty()) {
                    Log.w(PANE_STATE_TAG, "Failed to restore tabs, falling back to default")
                }
                val active = (restored["active"] as? Int) ?: 0
                val effectiveTabs = if (restoredTabs.isEmpty()) {
                    listOf(TabState.create(basePath))
                } else restoredTabs
                PaneState(
                    defaultPath = basePath,
                    initialTabs = effectiveTabs,
                    initialActiveIndex = active.coerceIn(0, effectiveTabs.lastIndex)
                )
            }
        )

        fun initial(defaultPath: String) = PaneState(
            defaultPath = defaultPath,
            initialTabs = listOf(TabState.create(defaultPath)),
            initialActiveIndex = 0
        )
    }
}

private const val PANE_STATE_TAG = "PaneState"
