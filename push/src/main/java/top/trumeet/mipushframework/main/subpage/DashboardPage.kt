package top.trumeet.mipushframework.main.subpage

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nihility.service.PushServiceTimeline
import com.nihility.service.XMPushServiceListener.ConnectionStatus
import com.xiaomi.xmsf.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.trumeet.common.utils.Utils
import top.trumeet.mipushframework.component.PageColumn
import top.trumeet.mipushframework.component.RefreshableLazyColumn
import top.trumeet.mipushframework.component.SettingsGroup
import top.trumeet.mipushframework.component.SettingsRow
import top.trumeet.mipushframework.component.StatusDot
import top.trumeet.mipushframework.main.ConnectionStatusHolder
import top.trumeet.mipushframework.main.RegistrationStateStyle
import top.trumeet.mipushframework.utils.ParseUtils
import top.trumeet.ui.theme.Layout
import java.util.Date

private val UnknownColor = Color(0xFF9E9E9E)

/** How often the two durations on this page are recomputed while it is on screen. */
private const val DurationTickMillis = 30_000L

@Composable
fun Dashboard() {
    val context = LocalContext.current
    var info by remember { mutableStateOf<DashboardPageOperation.DashboardInfo?>(null) }
    // Deliberately not rememberSaveable: the instance below is only remembered, so
    // restoring a saved "already loaded" flag would leave the page empty forever
    // after the tab is left and entered again.
    var isNeedRefresh by remember { mutableStateOf(true) }
    // The durations come from plain fields rather than Compose state, so nothing would recompose
    // them on its own — they would sit frozen at whatever they read when the page was opened.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val refreshScope = rememberCoroutineScope { Dispatchers.IO }
    val onRefresh: (onRefreshed: () -> Unit) -> Unit = { onRefreshed ->
        refreshScope.launch {
            val loaded = DashboardPageOperation.load(context)
            withContext(Dispatchers.Main) {
                info = loaded
                isNeedRefresh = false
                onRefreshed()
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(DurationTickMillis)
            now = System.currentTimeMillis()
        }
    }

    Page {
        RefreshableLazyColumn(
            onRefresh,
            { false },
            onRefresh,
            isNeedRefresh,
            verticalArrangement = Arrangement.spacedBy(Layout.CardGap)
        ) {
            item { ConnectionCard(ConnectionStatusHolder.status, PushServiceTimeline.connectedSince, now) }
            item { CountersRow(info) }
            item {
                SettingsGroup(title = stringResource(R.string.dashboard_group_service)) {
                    ServiceRow(
                        label = stringResource(R.string.dashboard_xmpp_server),
                        value = ConnectionStatusHolder.host ?: info?.storedXmppServer
                    )
                    ServiceRow(
                        label = stringResource(R.string.dashboard_last_receive),
                        value = lastReceiveText(info)
                    )
                    ServiceRow(
                        label = stringResource(R.string.dashboard_service_uptime),
                        value = PushServiceTimeline.serviceStartedAt?.let { formatDuration(context, now - it) }
                    )
                }
            }
        }
    }
}

/**
 * Renders a span as the two coarsest units that carry meaning: "3 天 4 小时", "19 小时 42 分",
 * "5 分" — never a bare "10 小时 0 分 0 秒".
 */
private fun formatDuration(context: Context, millis: Long): String {
    val seconds = (millis / 1000).coerceAtLeast(0L)
    val days = seconds / 86_400
    val hours = seconds % 86_400 / 3_600
    val minutes = seconds % 3_600 / 60
    return when {
        days > 0 -> context.getString(R.string.duration_days_hours, days, hours)
        hours > 0 -> context.getString(R.string.duration_hours_minutes, hours, minutes)
        minutes > 0 -> context.getString(R.string.duration_minutes, minutes)
        else -> context.getString(R.string.duration_seconds, seconds)
    }
}

@Composable
private fun lastReceiveText(info: DashboardPageOperation.DashboardInfo?): String {
    val context = LocalContext.current
    return when {
        info == null -> stringResource(R.string.dashboard_value_none)
        info.lastReceiveTime == 0L -> stringResource(R.string.dashboard_value_never)
        else -> ParseUtils.getFriendlyDateString(Date(info.lastReceiveTime), Utils.getUTC(), context)
    }
}

/**
 * The one card that carries the colour of the page.
 *
 * The server address deliberately stays off this card: it already has a row of its own further
 * down, and repeating it here only made the two disagree when the connection was moving.
 */
@Composable
private fun ConnectionCard(status: ConnectionStatus?, connectedSince: Long?, now: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Layout.CardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(connectionColor(status), size = 14.dp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    stringResource(R.string.dashboard_connection),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(connectionLabel(status)),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (status == ConnectionStatus.connected && connectedSince != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(
                            R.string.dashboard_connection_since,
                            formatDuration(LocalContext.current, now - connectedSince)
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CountersRow(info: DashboardPageOperation.DashboardInfo?) {
    val notLoaded = stringResource(R.string.dashboard_value_none)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Layout.CardGap)
    ) {
        CounterCard(
            title = stringResource(R.string.dashboard_registered_apps),
            value = info?.registeredAppCount?.toString() ?: notLoaded,
            modifier = Modifier.weight(1f)
        )
        CounterCard(
            title = stringResource(R.string.dashboard_recent_push),
            value = info?.recentPushCount?.toString() ?: notLoaded,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CounterCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Layout.CardPadding)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** A read-only row: the label as the headline, the value as the supporting text. */
@Composable
private fun ServiceRow(label: String, value: String?) {
    SettingsRow(
        title = label,
        summary = value?.takeIf { it.isNotEmpty() } ?: stringResource(R.string.dashboard_value_none)
    )
}

private fun connectionColor(status: ConnectionStatus?): Color = when (status) {
    ConnectionStatus.connected -> RegistrationStateStyle.GreenColor
    ConnectionStatus.connecting -> RegistrationStateStyle.YellowColor
    ConnectionStatus.disconnected -> RegistrationStateStyle.ErrorColor
    else -> UnknownColor
}

private fun connectionLabel(status: ConnectionStatus?): Int = when (status) {
    ConnectionStatus.connected -> R.string.dashboard_connection_connected
    ConnectionStatus.connecting -> R.string.dashboard_connection_connecting
    ConnectionStatus.disconnected -> R.string.dashboard_connection_disconnected
    else -> R.string.dashboard_connection_unknown
}

@Preview(showBackground = true, device = Devices.PIXEL_3, showSystemUi = true)
@Composable
fun DashboardPreview() {
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    Utils.context = context
    Page {
        PageColumn {
            ConnectionCard(ConnectionStatus.connected, now - 19L * 3_600_000L, now)
            CountersRow(null)
            SettingsGroup(title = stringResource(R.string.dashboard_group_service)) {
                ServiceRow(stringResource(R.string.dashboard_xmpp_server), "mtalk.google.com:5222")
                ServiceRow(stringResource(R.string.dashboard_last_receive), null)
                ServiceRow(
                    stringResource(R.string.dashboard_service_uptime),
                    formatDuration(context, 3L * 86_400_000L + 4L * 3_600_000L)
                )
            }
        }
    }
}
