package com.droidexplorer.websim.ui

import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver

enum class PaneMode { SINGLE, DUAL }

/* ─────────────────────────────────────────────
 * PaneNavigator
 * ───────────────────────────────────────────── */
class PaneNavigator(
    startPath: String,
    backStack: List<String> = emptyList(),
    forwardStack: List<String> = emptyList()
) {

    var currentPath: String = startPath
        private set

    private val backStackDeque = ArrayDeque(backStack)
    private val forwardStackDeque = ArrayDeque(forwardStack)

    fun canGoBack(): Boolean = backStackDeque.isNotEmpty()
    fun canGoForward(): Boolean = forwardStackDeque.isNotEmpty()

    fun navigateTo(newPath: String) {
        val normalized =
            if (newPath.startsWith("torbox:")) "torbox:" else newPath

        if (normalized == currentPath) return

        backStackDeque.addLast(currentPath)
        forwardStackDeque.clear()
        currentPath = normalized
    }

    fun goBack() {
        if (backStackDeque.isNotEmpty()) {
            forwardStackDeque.addLast(currentPath)
            currentPath = backStackDeque.removeLast()
        }
    }

    fun goForward() {
        if (forwardStackDeque.isNotEmpty()) {
            backStackDeque.addLast(currentPath)
            currentPath = forwardStackDeque.removeLast()
        }
    }

    fun backStackSnapshot(): List<String> = backStackDeque.toList()
    fun forwardStackSnapshot(): List<String> = forwardStackDeque.toList()
}

/* ─────────────────────────────────────────────
 * PaneState
 * ───────────────────────────────────────────── */
class PaneState private constructor(
    private val defaultPath: String,
    initialTabs: List<TabState>,
    initialActiveIndex: Int
) {

    private val _tabs = mutableStateListOf<TabState>().apply {
        addAll(initialTabs.ifEmpty { listOf(TabState.create(defaultPath)) })
    }

    val tabs: List<TabState>
        get() = _tabs

    private val _activeTabIndex =
        mutableIntStateOf(initialActiveIndex.coerceIn(0, _tabs.lastIndex))

    val activeTabIndex: Int
        get() = _activeTabIndex.intValue

    private val _path =
        mutableStateOf(_tabs[activeTabIndex].path)

    val path: String
        get() = _path.value

    val activeTab: TabState?
        get() = _tabs.getOrNull(activeTabIndex)

    fun canGoBack(): Boolean = activeTab?.canGoBack() == true
    fun canGoForward(): Boolean = activeTab?.canGoForward() == true

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
        _activeTabIndex.intValue = _tabs.lastIndex
        syncPath()
    }

    fun closeTab(index: Int) {
        if (_tabs.size <= 1 || index !in _tabs.indices) return
        _tabs.removeAt(index)
        _activeTabIndex.intValue =
            _activeTabIndex.intValue.coerceAtMost(_tabs.lastIndex)
        syncPath()
    }

    fun selectTab(index: Int) {
        if (index !in _tabs.indices) return
        _activeTabIndex.intValue = index
        syncPath()
    }

    private fun syncPath() {
        _path.value = activeTab?.path ?: defaultPath
    }

    fun snapshot(): Map<String, Any> = mapOf(
        "default" to defaultPath,
        "active" to activeTabIndex,
        "tabs" to tabs.map { it.toSaverMap() }
    )

    companion object {

        fun saver(defaultPath: String): Saver<PaneState, Any> = mapSaver(
            save = { it.snapshot() },
            restore = { restored ->
                val basePath =
                    restored["default"] as? String ?: defaultPath

                val rawTabs =
                    restored["tabs"] as? List<*>

                val restoredTabs =
                    rawTabs?.mapNotNull { TabState.fromSaved(it) }.orEmpty()

                val active =
                    restored["active"] as? Int ?: 0

                if (rawTabs?.isNotEmpty() == true && restoredTabs.isEmpty()) {
                    Log.w("PaneState", "Failed to restore tabs, falling back")
                }

                val effectiveTabs =
                    if (restoredTabs.isEmpty())
                        listOf(TabState.create(basePath))
                    else restoredTabs

                PaneState(
                    defaultPath = basePath,
                    initialTabs = effectiveTabs,
                    initialActiveIndex =
                        active.coerceIn(0, effectiveTabs.lastIndex)
                )
            }
        )

        fun initial(defaultPath: String) =
            PaneState(
                defaultPath = defaultPath,
                initialTabs = listOf(TabState.create(defaultPath)),
                initialActiveIndex = 0
            )
    }
}