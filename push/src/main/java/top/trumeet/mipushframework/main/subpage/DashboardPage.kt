package top.trumeet.mipushframework.main.subpage

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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nihility.service.XMPushServiceListener.ConnectionStatus
import com.xiaomi.xmsf.R
import kotlinx.coroutines.Dispatchers
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

@Composable
fun Dashboard() {
    val context = LocalContext.current
    var info by remember { mutableStateOf<DashboardPageOperation.DashboardInfo?>(null) }
    // Deliberately not rememberSaveable: the instance below is only remembered, so
    // restoring a saved "already loaded" flag would leave the page empty forever
    // after the tab is left and entered again.
    var isNeedRefresh by remember { mutableStateOf(true) }

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

    Page {
        RefreshableLazyColumn(
            onRefresh,
            { false },
            onRefresh,
            isNeedRefresh,
            verticalArrangement = Arrangement.spacedBy(Layout.CardGap)
        ) {
            item { ConnectionCard(ConnectionStatusHolder.status) }
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
                }
            }
            item {
                SettingsGroup(title = stringResource(R.string.dashboard_registration)) {
                    CredentialRow(stringResource(R.string.dashboard_device_id), info?.deviceId, info != null)
                    CredentialRow(
                        stringResource(R.string.dashboard_registration_id),
                        info?.registrationId,
                        info != null
                    )
                }
            }
        }
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
private fun ConnectionCard(status: ConnectionStatus?) {
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

@Composable
private fun CredentialRow(label: String, value: String?, loaded: Boolean) {
    val valid = !value.isNullOrEmpty()
    val text = when {
        !loaded -> stringResource(R.string.dashboard_value_none)
        valid -> value!!
        else -> stringResource(R.string.dashboard_value_invalid)
    }
    SettingsRow(
        title = label,
        summary = text,
        colors = androidx.compose.material3.ListItemDefaults.colors(
            containerColor = Color.Transparent,
            supportingColor = when {
                !loaded -> MaterialTheme.colorScheme.onSurfaceVariant
                valid -> MaterialTheme.colorScheme.onSurface
                else -> RegistrationStateStyle.ErrorColor
            }
        )
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
    Utils.context = LocalContext.current
    Page {
        PageColumn {
            ConnectionCard(ConnectionStatus.connected)
            CountersRow(null)
            SettingsGroup(title = stringResource(R.string.dashboard_group_service)) {
                ServiceRow(stringResource(R.string.dashboard_xmpp_server), "mtalk.google.com:5222")
                ServiceRow(stringResource(R.string.dashboard_last_receive), null)
            }
            SettingsGroup(title = stringResource(R.string.dashboard_registration)) {
                CredentialRow(stringResource(R.string.dashboard_device_id), null, false)
                CredentialRow(stringResource(R.string.dashboard_registration_id), null, false)
            }
        }
    }
}
