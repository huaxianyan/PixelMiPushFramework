package top.trumeet.mipushframework.main.subpage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.nihility.Global
import com.xiaomi.xmsf.R
import top.trumeet.mipushframework.component.SearchBar

/**
 * The second level of the event pages: every event of a single application.
 *
 * This destination owns its scaffold and system-bar insets, including during predictive back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageEventsPage(packageName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val appName = remember(packageName) {
        Global.ApplicationNameCache().getAppName(context, packageName).toString()
    }
    var query by rememberSaveable(packageName) { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(appName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).consumeWindowInsets(padding).imePadding()
        ) {
            SearchBar(stringResource(R.string.action_search)) { query = it }
            EventList(query, packageName)
        }
    }
}
