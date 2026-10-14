package top.trumeet.mipushframework.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
private const val PackageEventsRoutePrefix = "package_events"
private const val PackageEventsRoute = "$PackageEventsRoutePrefix/{packageName}"
private const val PackageNameArgument = "packageName"

private fun packageEventsRoute(packageName: String) = "$PackageEventsRoutePrefix/$packageName"

private val mainPageUtils1 = MainPageUtils()

class MainPage : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mainPageUtils1.initOnCreate(applicationContext) { status, host ->
            ConnectionStatusHolder.update(status, host)
        }
        setContent {
            Main(Screen.Dashboard.route.toString()) { navController ->
                composable(Screen.Dashboard.route.toString()) { Dashboard() }
                composable(Screen.Events.route.toString()) {
                    PackageEventList { packageName ->
                        navController.navigate(packageEventsRoute(packageName))
                    }
                }
                composable(Screen.Apps.route.toString()) {
                    Column {
                        var query by rememberSaveable { mutableStateOf("") }
                        SearchBar(stringResource(R.string.action_search)) { query = it }
                        ApplicationList(query)
                    }
                }
                composable(Screen.Settings.route.toString()) { Settings() }
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
fun BottomNavigationBar(navController: NavController, currentRoute: String?) {
    val items = listOf(
        Screen.Dashboard, Screen.Events, Screen.Apps, Screen.Settings
    )

    NavigationBar {
        items.forEach { screen ->
            val name = stringResource(screen.route)
            NavigationBarItem(
                icon = { Icon(painterResource(id = screen.icon), contentDescription = null) },
                label = { Text(name) },
                selected = currentRoute == screen.route.toString(),
                onClick = {
                    navController.navigate(screen.route.toString()) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
        }
    }
}

/** Only hierarchy changes move the full viewport; the tab shell is one root destination. */
@Composable
private fun Main(
    startDestination: String,
    navContent: NavGraphBuilder.(NavController) -> Unit
) {
    val navController = rememberNavController()
    Theme {
        NavHost(
            navController = navController,
            startDestination = MainTabsRoute,
            modifier = Modifier.fillMaxSize(),
            enterTransition = Motion.Enter,
            exitTransition = Motion.Exit,
            popEnterTransition = Motion.PopEnter,
            popExitTransition = Motion.PopExit
        ) {
            composable(MainTabsRoute) {
                MainTabs(startDestination) { navContent(navController) }
            }
            composable(
                route = PackageEventsRoute,
                arguments = listOf(navArgument(PackageNameArgument) { type = NavType.StringType })
            ) { entry ->
                PackageEventsPage(
                    packageName = entry.arguments?.getString(PackageNameArgument).orEmpty(),
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/** Same-level selection keeps both bars stationary and restores each tab's navigation state. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTabs(
    startDestination: String,
    navContent: NavGraphBuilder.() -> Unit
) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        bottomBar = { BottomNavigationBar(navController, entry?.destination?.route) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize().padding(padding)
                .consumeWindowInsets(padding).imePadding(),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
            builder = navContent
        )
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
)
@Composable
private fun MainDashboardPreview() {
    Main(Screen.Dashboard.route.toString()) { _ ->
        composable(Screen.Dashboard.route.toString()) {
            DashboardPreview()
        }
        composable(Screen.Events.route.toString()) { }
        composable(Screen.Apps.route.toString()) { }
        composable(Screen.Settings.route.toString()) { }
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
)
@Composable
private fun MainEventsPreview() {
    Main(Screen.Events.route.toString()) { _ ->
        composable(Screen.Dashboard.route.toString()) { }
        composable(Screen.Events.route.toString()) {
            PackageEventListPreview()
        }
        composable(Screen.Apps.route.toString()) { }
        composable(Screen.Settings.route.toString()) { }
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
)
@Composable
private fun MainAppsPreview() {
    Main(Screen.Apps.route.toString()) { _ ->
        composable(Screen.Dashboard.route.toString()) { }
        composable(Screen.Events.route.toString()) { }
        composable(Screen.Apps.route.toString()) {
            Column {
                val onValueChange: (String) -> Unit = {}
                SearchBar(stringResource(R.string.action_search), onValueChange)
                ApplicationListPreview()
            }
        }
        composable(Screen.Settings.route.toString()) { }
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
)
@Composable
private fun MainSettingsPreview() {
    Main(Screen.Settings.route.toString()) { _ ->
        composable(Screen.Dashboard.route.toString()) { }
        composable(Screen.Events.route.toString()) { }
        composable(Screen.Apps.route.toString()) { }
        composable(Screen.Settings.route.toString()) {
            SettingsPagePreview()
        }
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
)
@Composable
private fun MainDialogPreview() {
    Main(Screen.Events.route.toString()) { _ ->
        composable(Screen.Dashboard.route.toString()) { }
        composable(Screen.Events.route.toString()) {
            EventDetailsDialogPreview()
        }
        composable(Screen.Apps.route.toString()) { }
        composable(Screen.Settings.route.toString()) { }
    }
}
