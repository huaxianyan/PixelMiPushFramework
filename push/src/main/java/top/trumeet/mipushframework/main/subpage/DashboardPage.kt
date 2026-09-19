package top.trumeet.mipushframework.main.subpage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import top.trumeet.mipushframework.component.RefreshableLazyColumn
import top.trumeet.mipushframework.main.ConnectionStatusHolder
import top.trumeet.mipushframework.main.RegistrationStateStyle
import top.trumeet.mipushframework.utils.ParseUtils
import java.util.Date

private val UnknownColor = Color(0xFF9E9E9E)
private val CardPadding = 12.dp

@Composable
fun Dashboard() {
    val context = LocalContext.current
    var info by remember { mutableStateOf<DashboardPageOperation.DashboardInfo?>(null) }
    var isNeedRefresh by rememberSaveable { mutableStateOf(true) }

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
        RefreshableLazyColumn(onRefresh, { false }, onRefresh, isNeedRefresh) {
            item {
                ConnectionCard(ConnectionStatusHolder.status, ConnectionStatusHolder.host)
            }
            item {
                CountersCard(info)
            }
            item {
                LastReceiveCard(info)
            }
            item {
                XmppServerCard(ConnectionStatusHolder.host, info?.storedXmppServer)
            }
            item {
                RegistrationCard(info)
            }
        }
    }
}

@Composable
private fun DashboardCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CardPadding, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ConnectionCard(status: ConnectionStatus?, host: String?) {
    DashboardCard(stringResource(R.string.dashboard_connection)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(connectionColor(status))
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    stringResource(connectionLabel(status)),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    host ?: stringResource(R.string.dashboard_value_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CountersCard(info: DashboardPageOperation.DashboardInfo?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = CardPadding, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CounterCard(
            title = stringResource(R.string.dashboard_registered_apps),
            value = info?.registeredAppCount?.toString() ?: "-",
            modifier = Modifier.weight(1f)
        )
        CounterCard(
            title = stringResource(R.string.dashboard_recent_push),
            value = info?.recentPushCount?.toString() ?: "-",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CounterCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LastReceiveCard(info: DashboardPageOperation.DashboardInfo?) {
    val context = LocalContext.current
    val lastReceiveTime = info?.lastReceiveTime ?: 0L

    DashboardCard(stringResource(R.string.dashboard_last_receive)) {
        Text(
            if (lastReceiveTime == 0L) stringResource(R.string.dashboard_value_never)
            else ParseUtils.getFriendlyDateString(
                Date(lastReceiveTime), Utils.getUTC(), context
            ),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun XmppServerCard(currentHost: String?, storedXmppServer: String?) {
    val server = if (currentHost.isNullOrEmpty()) storedXmppServer else currentHost
    DashboardCard(stringResource(R.string.dashboard_xmpp_server)) {
        Text(
            if (server.isNullOrEmpty()) stringResource(R.string.dashboard_value_none) else server,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RegistrationCard(info: DashboardPageOperation.DashboardInfo?) {
    DashboardCard(stringResource(R.string.dashboard_registration)) {
        CredentialRow(stringResource(R.string.dashboard_device_id), info?.deviceId)
        Spacer(Modifier.height(4.dp))
        CredentialRow(stringResource(R.string.dashboard_registration_id), info?.registrationId)
    }
}

@Composable
private fun CredentialRow(label: String, value: String?) {
    val valid = !value.isNullOrEmpty()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (valid) value!! else stringResource(R.string.dashboard_value_invalid),
            style = MaterialTheme.typography.bodyMedium,
            color = if (valid) MaterialTheme.colorScheme.onSurface
            else RegistrationStateStyle.ErrorColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
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
        Column(Modifier.verticalScroll(rememberScrollState())) {
            ConnectionCard(ConnectionStatus.connected, "mtalk.google.com:5222")
            CountersCard(null)
            LastReceiveCard(null)
            XmppServerCard("mtalk.google.com:5222", null)
            RegistrationCard(null)
        }
    }
}
