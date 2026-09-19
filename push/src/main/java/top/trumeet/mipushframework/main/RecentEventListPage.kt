package top.trumeet.mipushframework.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.xiaomi.xmsf.R
import top.trumeet.mipushframework.component.SearchBar
import top.trumeet.mipushframework.main.subpage.EventList
import top.trumeet.ui.theme.Theme

class RecentEventListPage : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val packageName = intent.dataString!!
        setContent {
            Theme {
                RecentEventList(packageName, onBack = { finish() })
            }
        }
    }
}

/** The recent activity of one application, opened from that application's own page. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentEventList(packageName: String, onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recent_activity_title)) },
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
        Column(Modifier.padding(padding)) {
            SearchBar(stringResource(R.string.action_search)) { query = it }
            EventList(query, packageName)
        }
    }
}
