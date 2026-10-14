package top.trumeet.mipushframework.wizard

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.xiaomi.xmsf.R
import top.trumeet.mipushframework.component.MarkdownView
import top.trumeet.mipushframework.component.PageColumn
import top.trumeet.mipushframework.component.SettingsGroup
import top.trumeet.mipushframework.component.StatusDot
import top.trumeet.mipushframework.main.RegistrationStateStyle
import top.trumeet.mipushframework.wizard.permission.AlertWindowPermissionInfo
import top.trumeet.mipushframework.wizard.permission.PermissionInfo
import top.trumeet.mipushframework.wizard.permission.PermissionOperator
import top.trumeet.mipushframework.wizard.permission.RequestIgnoreBatteryOptimizationsPermissionInfo
import top.trumeet.mipushframework.wizard.permission.UsageStatsPermissionInfo
import top.trumeet.ui.theme.Layout
import top.trumeet.ui.theme.Theme

class RequestPermissionPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Theme {
                PermissionMainPage(onFinish = { WizardSPUtils.finishWizard(this) })
            }
        }
    }
}

/**
 * The whole first-run setup on a single page.
 *
 * Every permission is a row of its own with the button that asks for it, and the row re-reads
 * its own state whenever the app comes back to the foreground, so granting something in the
 * system settings needs no "next" to be pressed. When nothing is left the finish button lights
 * up and the wizard is over.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionMainPage(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val entries = remember(context) { permissionEntries(context) }
    val granted = remember(entries) {
        mutableStateListOf<Boolean>().apply {
            addAll(entries.map { it.operator.isPermissionGranted() })
        }
    }

    val notificationGranted = remember(context) {
        mutableStateOf(hasNotificationPermission(context))
    }
    val notificationDenied = rememberSaveable { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        notificationGranted.value = it
        notificationDenied.value = !it
    }

    RecheckPermissionsOnResume(entries, granted) {
        notificationGranted.value = hasNotificationPermission(context)
    }

    val allGranted = granted.isNotEmpty() && granted.all { it } && notificationGranted.value

    Theme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text(stringResource(R.string.app_name)) })
            }
        ) { padding ->
            PageColumn(contentPadding = padding) {
                WelcomeCard()

                SettingsGroup(title = stringResource(R.string.wizard_permissions_title)) {
                    entries.forEachIndexed { index, entry ->
                        PermissionRow(
                            title = entry.title,
                            description = entry.description,
                            granted = granted.getOrElse(index) { false },
                            onRequest = entry.operator::requestPermission
                        )
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionRow(
                            title = stringResource(R.string.wizard_notification_permission),
                            description = stringResource(R.string.wizard_notification_permission_description),
                            granted = notificationGranted.value,
                            onRequest = {
                                if (notificationDenied.value && !ActivityCompat.shouldShowRequestPermissionRationale(
                                        context as Activity, Manifest.permission.POST_NOTIFICATIONS
                                    )) {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    )
                                } else {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )
                    }
                }

                Column {
                    Button(
                        onClick = onFinish,
                        enabled = allGranted,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.wizard_finish),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (!allGranted) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.wizard_finish_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Layout.CardPadding)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(R.mipmap.ic_launcher),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.large)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(16.dp))
            MarkdownView(
                stringResource(R.string.wizard_descr),
                textSize = MaterialTheme.typography.bodyMedium.fontSize.value
            )
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    ListItem(
        leadingContent = {
            StatusDot(if (granted) RegistrationStateStyle.GreenColor else NotGrantedColor)
        },
        headlineContent = {
            Text(title, style = MaterialTheme.typography.titleMedium)
        },
        supportingContent = {
            Text(description, style = MaterialTheme.typography.bodyMedium)
        },
        trailingContent = {
            if (granted) {
                Text(
                    text = stringResource(R.string.wizard_permission_granted),
                    style = MaterialTheme.typography.labelLarge,
                    color = RegistrationStateStyle.GreenColor
                )
            } else {
                FilledTonalButton(onClick = onRequest) {
                    Text(stringResource(R.string.wizard_permission_grant))
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

/** Re-reads every permission whenever the app returns to the foreground. */
@Composable
private fun RecheckPermissionsOnResume(
    entries: List<PermissionEntry>,
    granted: MutableList<Boolean>,
    recheckNotifications: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, entries) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                entries.forEachIndexed { index, entry ->
                    val value = entry.operator.isPermissionGranted()
                    if (granted.getOrNull(index) != value) {
                        granted[index] = value
                    }
                }
                recheckNotifications()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private val NotGrantedColor = Color(0xFF9E9E9E)

private class PermissionEntry(
    val title: String,
    val description: String,
    val operator: PermissionOperator
)

private fun PermissionInfo.toEntry() = PermissionEntry(
    title = permissionTitle,
    description = permissionDescription,
    operator = permissionOperator
)

private fun permissionEntries(context: Context): List<PermissionEntry> {
    val entries = mutableListOf<PermissionEntry>()
    entries.add(UsageStatsPermissionInfo(context).toEntry())
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        entries.add(RequestIgnoreBatteryOptimizationsPermissionInfo(context).toEntry())
        entries.add(AlertWindowPermissionInfo(context).toEntry())
    }
    return entries
}

@Preview(showBackground = true)
@Composable
fun PermissionMainPagePreview() {
    Theme {
        PageColumn {
            WelcomeCard()
            SettingsGroup(title = stringResource(R.string.wizard_permissions_title)) {
                PermissionRow(
                    title = stringResource(R.string.wizard_title_stats_permission),
                    description = stringResource(R.string.wizard_title_stats_permission_text),
                    granted = true,
                    onRequest = UsageStatsPermissionInfo(LocalContext.current).permissionOperator::requestPermission
                )
                PermissionRow(
                    title = stringResource(R.string.wizard_title_alert_window_permission),
                    description = stringResource(R.string.wizard_title_alert_window_text),
                    granted = false,
                    onRequest = AlertWindowPermissionInfo(LocalContext.current).permissionOperator::requestPermission
                )
            }
        }
    }
}
