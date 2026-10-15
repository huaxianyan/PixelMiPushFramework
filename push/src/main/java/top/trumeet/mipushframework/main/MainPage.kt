package top.trumeet.mipushframework.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xiaomi.xmsf.R
import top.trumeet.mipushframework.MainPageUtils
import top.trumeet.mipushframework.component.SearchBar
import top.trumeet.mipushframework.main.subpage.ApplicationList
import top.trumeet.mipushframework.main.subpage.ApplicationListPreview
import top.trumeet.mipushframework.main.subpage.Dashboard
import top.trumeet.mipushframework.main.subpage.DashboardPreview
import top.trumeet.mipushframework.main.subpage.EventDetailsDialogPreview
import top.trumeet.mipushframework.main.subpage.PackageEventList
import top.trumeet.mipushframework.main.subpage.PackageEventListPreview
import top.trumeet.mipushframework.main.subpage.PackageEventsPage
import top.trumeet.mipushframework.main.subpage.Settings
import top.trumeet.mipushframework.main.subpage.SettingsPagePreview
import top.trumeet.ui.theme.Theme
import top.trumeet.ui.theme.Motion

private const val MainTabsRoute = "main_tabs"
private val mainPageUtils1 = MainPageUtils()

class MainPage : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mainPageUtils1.initOnCreate(applicationContext) { status, host ->
            ConnectionStatusHolder.update(status, host)
        }
        setContent {
            Main(Screen.Dashboard.route) { tab, navController ->
                when (tab) {
                    Screen.Dashboard.route -> Dashboard()
                    Screen.Events.route -> PackageEventList { packageName ->
                        navController.navigate(Pages.events(packageName))
                    }
                    Screen.Apps.route -> Column {
                        var query by rememberSaveable { mutableStateOf("") }
                        SearchBar(stringResource(R.string.action_search)) { query = it }
                        ApplicationList(query)
                    }
                    Screen.Settings.route -> Settings()
                }
            }
        }
    }
}

private sealed class Screen(val route: Int, val icon: Int) {
    object Dashboard :
        Screen(R.string.main_dashboard, R.drawable.ic_dashboard_black_24dp)

    object Events : Screen(R.string.main_event, R.drawable.ic_event_note_black_24dp)
    object Apps : Screen(R.string.main_apps, R.drawable.ic_apps_black_24dp)
    object Settings : Screen(R.string.main_settings, R.drawable.ic_settings_black_24dp)
}

@Composable
private fun BottomNavigationBar(selectedTab: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        Screen.Dashboard, Screen.Events, Screen.Apps, Screen.Settings
    )

    NavigationBar {
        items.forEach { screen ->
            val name = stringResource(screen.route)
            NavigationBarItem(
                icon = { Icon(painterResource(id = screen.icon), contentDescription = null) },
                label = { Text(name) },
                selected = selectedTab == screen.route,
                onClick = { onSelect(screen.route) })
        }
    }
}

/** Only hierarchy changes move the full viewport; the tab shell is one root destination. */
@Composable
private fun Main(
    startDestination: Int,
    content: @Composable (Int, NavController) -> Unit
) {
    val navController = rememberNavController()
    val moduleController = rememberModuleController()
    Theme {
        CompositionLocalProvider(
            LocalPageNavigation provides { route -> navController.navigate(route) },
            LocalModuleController provides moduleController
        ) {
            NavHost(
                navController = navController,
                startDestination = MainTabsRoute,
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                enterTransition = Motion.Enter,
                exitTransition = Motion.Exit,
                popEnterTransition = Motion.PopEnter,
                popExitTransition = Motion.PopExit
            ) {
                composable(MainTabsRoute) {
                    MainTabs(startDestination) { tab -> content(tab, navController) }
                }
                composable(
                    route = Pages.Events,
                    arguments = listOf(navArgument(Pages.PackageName) { type = NavType.StringType })
                ) { entry ->
                    PackageEventsPage(
                        packageName = requireNotNull(entry.arguments?.getString(Pages.PackageName)),
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Pages.Module) {
                    ModuleSettings(onBack = { navController.popBackStack() })
                }
                composable(Pages.Advanced) {
                    AdvancedSettings(onBack = { navController.popBackStack() })
                }
                composable(Pages.Help) {
                    HelpHost(onBack = { navController.popBackStack() })
                }
                composable(
                    Pages.Application,
                    arguments = listOf(
                        navArgument(Pages.PackageName) { type = NavType.StringType },
                        navArgument(Pages.IgnoreNotRegistered) {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) { entry ->
                    ApplicationInfoPage(
                        packageName = requireNotNull(entry.arguments?.getString(Pages.PackageName)),
                        ignoreNotRegistered = entry.arguments?.getBoolean(Pages.IgnoreNotRegistered) == true,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    Pages.Recent,
                    arguments = listOf(navArgument(Pages.PackageName) { type = NavType.StringType })
                ) { entry ->
                    RecentEventList(
                        packageName = requireNotNull(entry.arguments?.getString(Pages.PackageName)),
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

/** Peer selections own saved content state, not a second hierarchy back stack. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTabs(
    startDestination: Int,
    content: @Composable (Int) -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(startDestination) }
    val stateHolder = rememberSaveableStateHolder()
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    // A previewed root is STARTED: only the resumed root may handle peer-to-home Back.
    BackHandler(
        enabled = lifecycleState == Lifecycle.State.RESUMED && selectedTab != Screen.Dashboard.route
    ) {
        selectedTab = Screen.Dashboard.route
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        bottomBar = { BottomNavigationBar(selectedTab) { selectedTab = it } }
    ) { padding ->
        AnimatedContent(
            targetState = selectedTab,
            modifier = Modifier.fillMaxSize().padding(padding)
                .consumeWindowInsets(padding).imePadding(),
            transitionSpec = { Motion.TabEnter togetherWith Motion.TabExit },
            label = "main_tab"
        ) { tab ->
            stateHolder.SaveableStateProvider(tab) { content(tab) }
        }
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
)
@Composable
private fun MainDashboardPreview() {
    Main(Screen.Dashboard.route) { tab, _ ->
        if (tab == Screen.Dashboard.route) DashboardPreview()
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
)
@Composable
private fun MainEventsPreview() {
    Main(Screen.Events.route) { tab, _ ->
        if (tab == Screen.Events.route) PackageEventListPreview()
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
)
@Composable
private fun MainAppsPreview() {
    Main(Screen.Apps.route) { tab, _ ->
        if (tab == Screen.Apps.route) {
            Column {
                val onValueChange: (String) -> Unit = {}
                SearchBar(stringResource(R.string.action_search), onValueChange)
                ApplicationListPreview()
            }
        }
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
)
@Composable
private fun MainSettingsPreview() {
    Main(Screen.Settings.route) { tab, _ ->
        if (tab == Screen.Settings.route) SettingsPagePreview()
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
)
@Composable
private fun MainDialogPreview() {
    Main(Screen.Events.route) { tab, _ ->
        if (tab == Screen.Events.route) EventDetailsDialogPreview()
    }
}
