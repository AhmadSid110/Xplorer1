package com.droidexplorer.websim.ui

class PaneNavigator(startPath: String) {

    var currentPath: String = startPath
        private set

    private val backStack = ArrayDeque<String>()

    fun canGoBack(): Boolean = backStack.isNotEmpty()

    fun navigateTo(newPath: String) {
        backStack.addLast(currentPath)
        currentPath = newPath
    }

    fun goBack() {
        if (backStack.isNotEmpty()) {
            currentPath = backStack.removeLast()
        }
    }
}
