package top.trumeet.mipushframework.main.subpage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.nihility.Global
import com.xiaomi.xmsf.R
import top.trumeet.mipushframework.component.SearchBar

/**
 * The second level of the event pages: every event of a single application.
 *
 * The top bar of the shell is hidden on this route, so the one below carries the back arrow and
 * claims no window insets of its own — the shell already handed them down.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageEventsPage(packageName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val appName = remember(packageName) {
        Global.ApplicationNameCache().getAppName(context, packageName).toString()
    }
    var query by rememberSaveable(packageName) { mutableStateOf("") }

    Page {
        Column {
            TopAppBar(
                title = { Text(appName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
            SearchBar(stringResource(R.string.action_search)) { query = it }
            EventList(query, packageName)
        }
    }
}
