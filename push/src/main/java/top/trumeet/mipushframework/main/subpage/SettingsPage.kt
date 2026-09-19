package top.trumeet.mipushframework.main.subpage

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nihility.InternalMessenger
import com.xiaomi.push.service.XMPushServiceMessenger
import com.xiaomi.xmsf.BuildConfig
import com.xiaomi.xmsf.R
import com.xiaomi.xmsf.SettingUtils
import top.trumeet.common.utils.Utils
import top.trumeet.mipushframework.MainPageOperation
import top.trumeet.mipushframework.component.NavigationRow
import top.trumeet.mipushframework.component.PageColumn
import top.trumeet.mipushframework.component.SettingsGroup
import top.trumeet.mipushframework.component.SettingsItem
import top.trumeet.mipushframework.main.AdvancedSettingsPage
import top.trumeet.mipushframework.main.HelpPage
import top.trumeet.mipushframework.utils.ConfigurationDirectoryUtils
import top.trumeet.ui.theme.Theme

@Composable
fun Settings() {
    Page {
        PageColumn {
            ServiceConfigurationBlock()
            DebugBlock()
            AboutBlock()
        }
    }
}

@Composable
private fun ServiceConfigurationBlock() {
    val context = LocalContext.current

    SettingsGroup(title = stringResource(R.string.settings_service_setting)) {
        NavigationRow(
            title = stringResource(R.string.settings_service_advance_setting),
            summary = stringResource(R.string.settings_summary_service_advance_setting)
        ) {
            context.startActivity(Intent(context, AdvancedSettingsPage::class.java))
        }

        SetConfigurationsDirectory()
        SetXMPPServer(context)
    }
}

/**
 * The stored server plus, when they differ, the one the service is actually talking to.
 *
 * The receiver is remembered so that a recomposition does not register a second copy of it.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SetXMPPServer(context: Context) {
    val currentServer = remember { mutableStateOf("") }
    remember(context) {
        object : InternalMessenger(context) {
            init {
                register(IntentFilter(XMPushServiceMessenger.IntentSetConnectionStatus))
                addListener { intent: Intent ->
                    val host = intent.getStringExtra("host")
                    if (!host.isNullOrEmpty()) {
                        currentServer.value = host
                    }
                }

                send(Intent(XMPushServiceMessenger.IntentGetConnectionStatus))
            }
        }
    }
    // A stored value of "" means the same as never having set one — the service falls back to
    // its built-in server. Without this the summary opens with a blank line, because the
    // preference can hold an empty string rather than nothing at all.
    val storedServer = SettingUtils.getXMPPServer(context)?.takeIf { it.isNotBlank() }
    var text by remember { mutableStateOf(storedServer ?: "") }
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                text = storedServer ?: ""
            },
            title = { Text(stringResource(R.string.settings_XMPP_server)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(SettingUtils.getXMPPServerHint()) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    SettingUtils.setXMPPServer(context, text)
                    SettingUtils.sendXMPPReconnectRequest(context)
                    currentServer.value = text
                    showDialog = false
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }

    NavigationRow(
        title = stringResource(R.string.settings_XMPP_server),
        summary = buildString {
            val stored = storedServer
            if (stored == null) {
                append(stringResource(R.string.settings_XMPP_server_summary))
            } else {
                append(stringResource(R.string.settings_XMPP_server_summary_set, stored))
            }
            val running = currentServer.value
            if (running.isNotEmpty() && running != stored) {
                append('\n')
                append(
                    stringResource(
                        R.string.settings_XMPP_server_summary_current,
                        running
                    )
                )
            }
        }
    ) {
        text = storedServer ?: ""
        showDialog = true
    }
}

@Composable
private fun SetConfigurationsDirectory() {
    val context = LocalContext.current
    var selectedDirectoryUri by remember {
        mutableStateOf(
            SettingUtils.getConfigurationDirectory(
                context
            )
        )
    }
    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            selectedDirectoryUri = uri
            SettingUtils.setConfigurationDirectory(context, uri)
        }
    }
    NavigationRow(
        title = stringResource(R.string.settings_configuration_directory),
        summary = ConfigurationDirectoryUtils.displayName(context, selectedDirectoryUri)
    ) {
        openDocumentTreeLauncher.launch(null) // 启动文件选择器
    }
}

@Composable
private fun DebugBlock() {
    val context = LocalContext.current

    SettingsGroup(title = stringResource(R.string.settings_debug)) {
        SettingsItem(
            title = stringResource(R.string.settings_get_log),
            summary = stringResource(R.string.settings_get_log_summary)
        ) {
            SettingUtils.shareLogs(context)
        }

        SettingsItem(
            title = stringResource(R.string.try_to_force_register_all_applications)
        ) {
            SettingUtils.tryForceRegisterAllApplications()
        }
    }
}

@Composable
private fun AboutBlock() {
    val context = LocalContext.current
    val mainPageOperation = MainPageOperation(context)
    var showAbout by remember { mutableStateOf(false) }

    if (showAbout) {
        AboutDialog { showAbout = false }
    }

    SettingsGroup(title = stringResource(R.string.action_about)) {
        NavigationRow(
            title = stringResource(R.string.helplib_title)
        ) {
            context.startActivity(Intent(context, HelpPage::class.java))
        }

        NavigationRow(
            title = stringResource(R.string.action_update)
        ) {
            mainPageOperation.gotoGitHubReleasePage()
            Toast.makeText(context, R.string.update_toast, Toast.LENGTH_LONG).show()
        }

        SettingsItem(
            title = stringResource(R.string.action_version_info)
        ) {
            showAbout = true
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val versionInfo = remember {
        "name: ${BuildConfig.VERSION_NAME}\n" +
                "code: ${BuildConfig.VERSION_CODE}\n" +
                "flavor: ${BuildConfig.FLAVOR}\n" +
                "type: ${BuildConfig.BUILD_TYPE}"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Image(
                painterResource(R.mipmap.ic_launcher),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        },
        title = { Text(stringResource(R.string.app_name)) },
        text = {
            Column {
                Text(
                    versionInfo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.about_copyright),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.about_fork),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                        as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("version", versionInfo))
                onDismiss()
            }) {
                Text(stringResource(android.R.string.copy))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsPagePreview() {
    Utils.context = LocalContext.current
    Theme {
        PageColumn {
            ServiceConfigurationBlock()
            DebugBlock()
            AboutBlock()
        }
    }
}
