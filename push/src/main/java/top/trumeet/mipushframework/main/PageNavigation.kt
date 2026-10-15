package top.trumeet.mipushframework.main

import android.net.Uri
import androidx.compose.runtime.staticCompositionLocalOf

/** Navigation owned by the full-window host, not the same-level tab host. */
val LocalPageNavigation = staticCompositionLocalOf<(String) -> Unit> {
    { error("Page navigation must be provided by the main host") }
}

/** Destination patterns and argument names have one definition for all page entrances. */
object Pages {
    const val PackageName = "packageName"
    const val IgnoreNotRegistered = "ignoreNotRegistered"
    const val Advanced = "advanced_settings"
    const val Module = "module_settings"
    const val Help = "help"
    private const val EventsPrefix = "package_events"
    private const val ApplicationPrefix = "application_info"
    private const val RecentPrefix = "recent_events"
    const val Events = "$EventsPrefix/{$PackageName}"
    const val Application = "$ApplicationPrefix/{$PackageName}?$IgnoreNotRegistered={$IgnoreNotRegistered}"
    const val Recent = "$RecentPrefix/{$PackageName}"

    fun events(packageName: String) = "$EventsPrefix/${Uri.encode(packageName)}"
    fun application(packageName: String, ignoreNotRegistered: Boolean = false) =
        "$ApplicationPrefix/${Uri.encode(packageName)}?$IgnoreNotRegistered=$ignoreNotRegistered"
    fun recent(packageName: String) = "$RecentPrefix/${Uri.encode(packageName)}"
}
