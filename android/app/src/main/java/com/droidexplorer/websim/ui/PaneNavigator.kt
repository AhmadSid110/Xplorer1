package com.droidexplorer.websim.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class PaneMode { SINGLE, DUAL }

class PaneNavigator(startPath: String) {

    var currentPath: String = startPath
        private set

    private val backStack = ArrayDeque<String>()
    private val forwardStack = ArrayDeque<String>()

    fun canGoBack(): Boolean = backStack.isNotEmpty()
    fun canGoForward(): Boolean = forwardStack.isNotEmpty()

    fun navigateTo(newPath: String) {
        backStack.addLast(currentPath)
        forwardStack.clear()
        currentPath = newPath
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
            backStack.addLast(currentPath)
            forwardStack.clear()
            currentPath = path
        }
    }
}

class PaneState(startPath: String) {
    val navigator = PaneNavigator(startPath)
    var path: String by mutableStateOf(startPath)

    fun canGoBack() = navigator.canGoBack()
    fun canGoForward() = navigator.canGoForward()

    fun navigateTo(newPath: String) {
        navigator.navigateTo(newPath)
        path = navigator.currentPath
    }

    fun navigateToPath(newPath: String) {
        navigator.navigateToPath(newPath)
        path = navigator.currentPath
    }

    fun goBack() {
        navigator.goBack()
        path = navigator.currentPath
    }

    fun goForward() {
        navigator.goForward()
        path = navigator.currentPath
    }
}
