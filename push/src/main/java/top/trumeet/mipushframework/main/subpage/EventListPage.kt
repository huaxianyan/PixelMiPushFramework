package top.trumeet.mipushframework.main.subpage

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import top.trumeet.ui.theme.Layout
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elvishew.xlog.XLog
import com.xiaomi.xmsf.R
import top.trumeet.mipushframework.main.LocalPageNavigation
import top.trumeet.mipushframework.main.Pages
import com.xiaomi.xmsf.push.utils.RegSecUtils
import top.trumeet.common.Constants
import top.trumeet.common.utils.Utils
import top.trumeet.mipush.provider.entities.Event
import top.trumeet.mipush.provider.event.type.TypeFactory
import top.trumeet.mipushframework.component.AppIcon
import top.trumeet.mipushframework.component.EmptyHint
import top.trumeet.mipushframework.component.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

private val receiveDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

/** `disable` is what the dimmed card already says, so it is kept out of the overline. */
private const val DisabledOption = "disable"

@Composable
fun EventList(query: String = "", packageName: String = "") {
    Page {
        val context = LocalContext.current
        var clickedEvent by remember { mutableStateOf<EventInfoForDisplay?>(null) }

        clickedEvent?.let {
            EventDetailsDialog(clickedEvent!!) { clickedEvent = null }
        }

        EventList(onClick = { clickedEvent = it }, { beforeId, size ->
            val utils = EventListPageUtils(context)
            EventListPageUtils.getEventsById(beforeId, size, packageName, query).map {
                toEventInfoForDisplay(it, context, utils)
            }
        }, query, packageName)
    }
}

fun toEventInfoForDisplay(
    it: Event,
    context: Context,
    utils: EventListPageUtils
): EventInfoForDisplay {
    val type = TypeFactory.createForDisplay(it)

    val container = RegSecUtils.getContainerWithRegSec(it)
    val summary = type.getSummary(context).toString()
    val content = if (container != null)
        EventListPageUtils.getDecoratedSummary(
            summary,
            container
        )
    else summary
    return EventInfoForDisplay(
        id = it.id,
        packageName = it.pkg,
        configOptions = utils.getStatus(container),
        channel = utils.getStatusDescription(it),
        receiveDate = Date(it.date),
        title = type.getTitle(context).toString(),
        content = content,
        appName = "",
        event = it,
    )
}

@Composable
private fun EventDetailsDialog(
    clickedEvent: EventInfoForDisplay,
    content: String? = null,
    onDismiss: () -> Unit
) {
    var json by remember {
        mutableStateOf(
            content ?: EventListPageUtils.getJson(
                clickedEvent.event
            ).toString()
        )
    }
    val context = LocalContext.current

    val navigate = LocalPageNavigation.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val targetHeight = screenHeight * 0.9f

    AlertDialog(
        onDismiss,
        {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton({
                    json =
                        EventListPageUtils.getContent(
                            clickedEvent.event,
                            RegSecUtils.getContainerWithRegSec(clickedEvent.event)
                        )
                }) { Text(stringResource(R.string.action_configurate)) }

                TextButton({
                    EventListPageUtils.copyToClipboard(context, json)
                }) { Text(stringResource(android.R.string.copy)) }

                TextButton({
                    EventListPageUtils.mockMessage(
                        RegSecUtils.getContainerWithRegSec(
                            clickedEvent.event
                        )
                    )
                }) { Text(stringResource(R.string.action_notify)) }
            }
        },
        title = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.event_developer_info),
                    style = MaterialTheme.typography.titleLarge
                )

                TextButton({
                    onDismiss()
                    navigate(Pages.application(clickedEvent.packageName))
                }) { Text(stringResource(R.string.action_app_info)) }
            }
        },
        text = {
            TextView(json)
        },
        modifier = Modifier.heightIn(Dp.Unspecified, targetHeight)
    )
}

@Composable
fun EventList(
    onClick: (EventInfoForDisplay) -> Unit,
    getEvents: (beforeId: Long?, size: Int) -> List<EventInfoForDisplay>,
    query: String,
    packageName: String
) {
    key(query, packageName) {
        val pager = remember {
            Pager(
                PagingConfig(
                    pageSize = Constants.PAGE_SIZE,
                    initialLoadSize = Constants.PAGE_SIZE,
                    prefetchDistance = 3,
                    enablePlaceholders = false
                )
            ) { EventHistoryPagingSource(getEvents) }
        }
        val events = pager.flow.collectAsLazyPagingItems()
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            SwipeRefresh(
                state = rememberSwipeRefreshState(events.loadState.refresh is LoadState.Loading),
                onRefresh = { events.refresh() },
                modifier = Modifier.widthIn(max = Layout.ContentMaxWidth).fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = Layout.PageHorizontal,
                        vertical = Layout.PageVertical
                    ),
                    verticalArrangement = Arrangement.spacedBy(Layout.ListGap)
                ) {
                    if (events.loadState.refresh is LoadState.Error) {
                        item { HistoryLoadError { events.retry() } }
                    } else if (events.itemCount == 0 && events.loadState.refresh is LoadState.NotLoading) {
                        item { EmptyHint(stringResource(R.string.event_list_empty)) }
                    }
                    items(events.itemCount, key = events.itemKey { it.id }) { index ->
                        events[index]?.let { EventItem(it, onClick) }
                    }
                    when (events.loadState.append) {
                        is LoadState.Loading -> item {
                            Box(
                                Modifier.fillMaxWidth().padding(Layout.CardPadding),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator() }
                        }
                        is LoadState.Error -> item { HistoryLoadError { events.retry() } }
                        is LoadState.NotLoading -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryLoadError(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(Layout.CardPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.event_history_load_error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun EventItem(item: EventInfoForDisplay, onClick: (EventInfoForDisplay) -> Unit) {
    val disabled = item.configOptions.contains(DisabledOption)
    val alpha = if (disabled) 0.5f else 1f
    Card(
        onClick = { onClick(item) },
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
    ) {
        ListItem(
            overlineContent = { EventOverline(item) },
            headlineContent = { EventTitle(item) },
            supportingContent = { EventContent(item) },
            leadingContent = {
                AppIcon(item.packageName, item.appName, modifier = Modifier.size(40.dp))
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

/** Channel and time on one line, and only the options that add something. */
@Composable
private fun EventOverline(item: EventInfoForDisplay) {
    val options = item.configOptions.filterNot { it == DisabledOption }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = (options + item.channel).joinToString(" · "),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Text(
            text = receiveDateFormat.format(item.receiveDate),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EventTitle(item: EventInfoForDisplay) {
    Text(
        item.title,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun EventContent(item: EventInfoForDisplay) {
    Text(
        item.content,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
)
@Composable
fun EventDetailsDialogPreview() {
    EventDetailsDialog(
        EventInfoForDisplay(0, "", setOf(), "", Date(), "", ""),
        "sdfasdfsdfasdf"
    ) { }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
)
@Composable
fun EventListPreview() {
    XLog.init()
    Utils.context = LocalContext.current

    val getEvents = { _: Long?, _: Int ->
        listOf(
                EventInfoForDisplay(
                    1212, "123",
                    setOf("123", "456"), "c1", date(2025, 1, 1),
                    "title", "content"
                ),
                EventInfoForDisplay(
                    123, "123",
                    setOf("disable", "456"), "c1", date(2025, 1, 1),
                    "title", "content"
                )
        )
    }

    Page {
        EventList({ }, getEvents, "", "")
    }
}

private fun date(year: Int, month: Int, date: Int): Date =
    Calendar.getInstance().apply {
        clear()
        set(year, month - 1, date)
    }.time


data class EventInfoForDisplay(
    val id: Long,
    val packageName: String,
    val configOptions: Set<String>,
    val channel: String,
    val receiveDate: Date,
    val title: String,
    val content: String,
    val appName: String? = null,
    val event: Event = Event(),
) {
    // Paging diagnostics stringify the first and last row; message content stays out of that path.
    override fun toString(): String = "EventInfoForDisplay(id=$id, packageName=$packageName)"
}
