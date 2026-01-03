package com.droidexplorer.websim.ui

class TabState internal constructor(
    internal val navigator: PaneNavigator
) {
    val path: String
        get() = navigator.currentPath

    fun canGoBack() = navigator.canGoBack()
    fun canGoForward() = navigator.canGoForward()

    fun navigateTo(newPath: String) = navigator.navigateTo(newPath)
    fun navigateToPath(newPath: String) = navigator.navigateToPath(newPath)
    fun goBack() = navigator.goBack()
    fun goForward() = navigator.goForward()

    fun toSaverMap(): Map<String, Any> = mapOf(
        "current" to navigator.currentPath,
        "back" to navigator.backStackSnapshot(),
        "forward" to navigator.forwardStackSnapshot()
    )

    companion object {
        fun create(startPath: String): TabState = TabState(PaneNavigator(startPath))

        fun fromSaved(data: Any?): TabState? {
            val map = data as? Map<*, *> ?: return null
            val current = map["current"] as? String ?: return null
            val back = (map["back"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val forward = (map["forward"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            return TabState(PaneNavigator(current, back, forward))
        }
    }
}
