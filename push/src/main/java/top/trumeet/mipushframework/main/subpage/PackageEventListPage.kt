package top.trumeet.mipushframework.main.subpage

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
    var isNeedRefresh by rememberSaveable { mutableStateOf(true) }

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
                    item { EmptyHint() }
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
    Row(
        Modifier
            .clickable { onClick(item.packageName) }
            .fillMaxWidth()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(item.packageName, item.appName, modifier = Modifier.size(48.dp))
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(item.appName, style = MaterialTheme.typography.bodyLarge)
            Text(
                item.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                stringResource(R.string.event_count, item.count),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                ParseUtils.getFriendlyDateString(item.lastDate, Utils.getUTC(), context),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun EmptyHint() {
    Text(
        stringResource(R.string.event_list_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(showBackground = true, device = Devices.PIXEL_3, showSystemUi = true)
@Composable
fun PackageEventListPreview() {
    Utils.context = LocalContext.current
    Page {
        Column {
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
