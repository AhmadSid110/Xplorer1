package com.droidexplorer.websim.ui

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
