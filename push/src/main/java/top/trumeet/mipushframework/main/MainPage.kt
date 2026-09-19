package top.trumeet.mipushframework.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
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

private const val PackageEventsRoutePrefix = "package_events"
private const val PackageEventsRoute = "$PackageEventsRoutePrefix/{packageName}"
private const val PackageNameArgument = "packageName"

private fun packageEventsRoute(packageName: String) = "$PackageEventsRoutePrefix/$packageName"

private val mainPageUtils1 = MainPageUtils()

class MainPage : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        mainPageUtils1.initOnCreate(applicationContext) { status, host ->
            ConnectionStatusHolder.update(status, host)
        }
        setContent {
            Theme {
                window.navigationBarColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                    NavigationBarDefaults.Elevation
                ).toArgb()
            }
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
}

private sealed class Screen(val route: Int, val icon: Int) {
    object Dashboard :
        Screen(R.string.main_dashboard, R.drawable.ic_dashboard_black_24dp)

    object Events : Screen(R.string.main_event, R.drawable.ic_event_note_black_24dp)
    object Apps : Screen(R.string.main_apps, R.drawable.ic_apps_black_24dp)
    object Settings : Screen(R.string.main_settings, R.drawable.ic_settings_black_24dp)
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        Screen.Dashboard, Screen.Events, Screen.Apps, Screen.Settings
    )

    NavigationBar(Modifier.height(56.dp)) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            val name = stringResource(screen.route)
            NavigationBarItem(
                icon = { Icon(painterResource(id = screen.icon), contentDescription = name) },
                selected = currentRoute == screen.route.toString() ||
                        (screen is Screen.Events &&
                                currentRoute?.startsWith(PackageEventsRoutePrefix) == true),
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

@Composable
private fun Main(
    startDestination: String,
    navContent: NavGraphBuilder.(NavController) -> Unit
) {
    val navController = rememberNavController()

    Theme {
        Column(
            Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    navContent(navController)
                }
            }
            BottomNavigationBar(navController)
        }
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
)
@Composable
private fun MainDashboardPreview() {
    Main(Screen.Dashboard.route.toString()) {
        composable(Screen.Dashboard.route.toString()) { DashboardPreview() }
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
    Main(Screen.Events.route.toString()) {
        composable(Screen.Dashboard.route.toString()) { }
        composable(Screen.Events.route.toString()) { PackageEventListPreview() }
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
    Main(Screen.Apps.route.toString()) {
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
    Main(Screen.Settings.route.toString()) {
        composable(Screen.Dashboard.route.toString()) { }
        composable(Screen.Events.route.toString()) { }
        composable(Screen.Apps.route.toString()) { }
        composable(Screen.Settings.route.toString()) { SettingsPagePreview() }
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
)
@Composable
private fun MainDialogPreview() {
    Main(Screen.Events.route.toString()) {
        composable(Screen.Dashboard.route.toString()) { }
        composable(Screen.Events.route.toString()) { EventDetailsDialogPreview() }
        composable(Screen.Apps.route.toString()) { }
        composable(Screen.Settings.route.toString()) { }
    }
}
