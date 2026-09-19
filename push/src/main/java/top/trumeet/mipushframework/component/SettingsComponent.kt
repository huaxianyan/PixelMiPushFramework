package top.trumeet.mipushframework.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xiaomi.xmsf.R
import com.xiaomi.xmsf.utils.ConfigCenter

/**
 * One group of settings: a heading on the page background plus the card that holds the rows.
 *
 * A null [title] keeps the card but drops the heading, for the groups that are the only thing on
 * their page.
 */
@Composable
fun SettingsGroup(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        if (title != null) {
            SectionHeader(title)
        }
        SettingsCard(content = content)
    }
}

/**
 * A row of a [SettingsGroup].
 *
 * [content] draws a control on the right (a switch, a chip); [chevron] marks a row that opens
 * another page. When both are absent the row is a plain action.
 */
@Composable
fun SettingsItem(
    title: String,
    summary: String? = null,
    content: (@Composable RowScope.() -> Unit)? = null,
    enabled: Boolean = true,
    chevron: Boolean = false,
    onClick: () -> Unit
) {
    SettingsRow(
        title = title,
        summary = summary,
        enabled = enabled,
        onClick = onClick,
        trailing = when {
            content != null -> {
                {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        content()
                    }
                }
            }

            chevron -> {
                {
                    NavigationChevron()
                }
            }

            else -> null
        }
    )
}

@Composable
fun SettingsItem(
    title: String,
    summary: String,
    key: String,
    values: Array<String>,
    defaultValue: String
) {
    SettingsItem(
        title = title,
        summary = summary,
        confirmButton = {},
        content = { dismiss ->
            ItemLists(key, defaultValue, values, dismiss)
        }
    )
}

@Composable
fun SettingsItem(
    title: String,
    summary: String,
    confirmButton: @Composable (dismiss: () -> Unit) -> Unit,
    onDismiss: (() -> Unit)? = null,
    content: @Composable (dismiss: () -> Unit) -> Unit
) {
    var shouldShowDialog by remember { mutableStateOf(false) }
    SettingsItem(
        title = title,
        summary = summary
    ) {
        shouldShowDialog = true
    }
    if (shouldShowDialog) {
        val hideDialog = {
            shouldShowDialog = false
            onDismiss?.invoke()
            Unit
        }
        SettingsDialog(title, true, hideDialog, { confirmButton(hideDialog) }) {
            content(hideDialog)
        }
    }
}

@Composable
fun SettingsDialog(
    title: String,
    shouldShowDialog: Boolean,
    onDismiss: () -> Unit,
    confirmButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    if (!shouldShowDialog) return
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = confirmButton,
        title = { Text(title) },
        text = content
    )
}

@Composable
private fun ItemLists(
    key: String,
    defaultValue: String,
    values: Array<String>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val preferences = ConfigCenter.getSharedPreferences(context)
    val selected = preferences.getString(key, defaultValue)!!.toInt()

    LazyColumn(Modifier.heightIn(max = 400.dp)) {
        itemsIndexed(values) { index, item ->
            SettingsRow(
                title = item,
                modifier = Modifier.clickable {
                    preferences
                        .edit()
                        .putString(key, index.toString())
                        .apply()
                    onDismiss()
                },
                leading = { RadioButton(index == selected, onClick = null) }
            )
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    summary: String? = null,
    key: String,
    defaultValue: Boolean,
    enabled: Boolean = true,
    onClick: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val preferences = ConfigCenter.getSharedPreferences(context)
    var checked by remember { mutableStateOf(preferences.getBoolean(key, defaultValue)) }
    SettingsItem(title = title, summary = summary, checked = checked, enabled = enabled) {
        preferences.edit().putBoolean(key, !checked).apply()
        checked = !checked
        onClick?.invoke(checked)
    }
}

@Composable
fun SettingsItem(
    title: String,
    summary: String? = null,
    enabled: Boolean = true,
    checked: Boolean,
    onClick: () -> Unit
) {
    SettingsItem(
        title = title,
        summary = summary,
        content = {
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = null
            )
        },
        enabled = enabled,
        onClick = onClick
    )
}

@Preview(showBackground = true)
@Composable
fun InfoDialogPreview() {
    SettingsDialog(
        title = stringResource(R.string.pref_title_access_mode),
        shouldShowDialog = true,
        {}, {}
    ) {
        ItemLists(
            "AccessMode",
            "0",
            stringArrayResource(R.array.pref_title_access_mode_list_titles)
        ) { }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsItemPreview() {
    PageColumn {
        SettingsGroup(stringResource(R.string.settings_options)) {
            SettingsItem(
                title = stringResource(R.string.settings_start_foreground_service),
                summary = stringResource(R.string.settings_start_foreground_service_summary),
                key = "StartForegroundService",
                defaultValue = false,
                enabled = false
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SingleLineSettingsItemPreview() {
    PageColumn {
        SettingsGroup(stringResource(R.string.settings_options)) {
            SettingsItem(
                title = stringResource(R.string.settings_start_foreground_service),
                chevron = true
            ) {}
        }
    }
}
