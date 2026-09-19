package top.trumeet.mipushframework.main.subpage

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nihility.Global
import com.xiaomi.xmsf.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.trumeet.common.utils.Utils
import top.trumeet.mipush.provider.db.EventDb
import top.trumeet.mipushframework.component.AppIcon
import top.trumeet.mipushframework.component.EmptyHint
import top.trumeet.mipushframework.component.PageColumn
import top.trumeet.mipushframework.component.RefreshableLazyColumn
import top.trumeet.mipushframework.component.SearchBar
import top.trumeet.mipushframework.utils.ParseUtils
import java.util.Date

data class PackageEventGroupForDisplay(
    val packageName: String,
    val appName: String,
    val count: Long,
    val lastDate: Date,
)

/**
 * The first level of the event pages: one entry per application, the one that received a push
 * most recently on top.
 */
@Composable
fun PackageEventList(onClick: (String) -> Unit) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var groups by remember { mutableStateOf<List<PackageEventGroupForDisplay>?>(null) }
    // Deliberately not rememberSaveable: `groups` is only remembered, so a restored
    // "already loaded" flag would keep this list empty after leaving the tab.
    var isNeedRefresh by remember { mutableStateOf(true) }

    val refreshScope = rememberCoroutineScope { Dispatchers.IO }
    val onRefresh: (onRefreshed: () -> Unit) -> Unit = { onRefreshed ->
        refreshScope.launch {
            val loaded = loadGroups(context)
            withContext(Dispatchers.Main) {
                groups = loaded
                isNeedRefresh = false
                onRefreshed()
            }
        }
    }

    val shown = groups?.filter { it.matches(query) }

    Page {
        Column {
            SearchBar(stringResource(R.string.action_search)) { query = it }
            RefreshableLazyColumn(onRefresh, { false }, onRefresh, isNeedRefresh) {
                if (shown.isNullOrEmpty()) {
                    item { EmptyHint(stringResource(R.string.event_list_empty)) }
                } else {
                    items(shown, { it.packageName }) {
                        PackageEventItem(it, onClick)
                    }
                }
            }
        }
    }
}

private fun loadGroups(context: Context): List<PackageEventGroupForDisplay> =
    EventDb.queryPackageGroups(EventListPageUtils.getDisplayTypes()).map {
        PackageEventGroupForDisplay(
            packageName = it.pkg,
            appName = Global.ApplicationNameCache().getAppName(context, it.pkg).toString(),
            count = it.count,
            lastDate = Date(it.lastDate),
        )
    }

private fun PackageEventGroupForDisplay.matches(query: String): Boolean {
    if (query.isBlank()) {
        return true
    }
    val keyword = query.lowercase()
    return packageName.lowercase().contains(keyword) || appName.lowercase().contains(keyword)
}

@Composable
private fun PackageEventItem(
    item: PackageEventGroupForDisplay,
    onClick: (String) -> Unit
) {
    val context = LocalContext.current
    Card(
        onClick = { onClick(item.packageName) },
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = {
                Text(
                    item.appName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Text(
                    item.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingContent = {
                AppIcon(item.packageName, item.appName, Modifier.size(40.dp))
            },
            trailingContent = {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.event_count, item.count),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        ParseUtils.getFriendlyDateString(item.lastDate, Utils.getUTC(), context),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_3, showSystemUi = true)
@Composable
fun PackageEventListPreview() {
    Utils.context = LocalContext.current
    Page {
        PageColumn {
            PackageEventItem(
                PackageEventGroupForDisplay("com.example.chat", "Example Chat", 128, Date()),
                {}
            )
            PackageEventItem(
                PackageEventGroupForDisplay("com.example.mail", "Example Mail", 7, Date()),
                {}
            )
        }
    }
}
