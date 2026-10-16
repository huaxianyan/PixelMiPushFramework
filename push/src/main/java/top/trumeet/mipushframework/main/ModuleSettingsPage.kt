package top.trumeet.mipushframework.main

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.neko7ina.pixelmipush.api.ModuleContract as Api
import com.xiaomi.xmsf.R
import kotlinx.coroutines.launch
import top.trumeet.mipushframework.component.NavigationRow
import top.trumeet.mipushframework.component.PageColumn
import top.trumeet.mipushframework.component.SettingsGroup
import top.trumeet.mipushframework.component.SettingsItem
import top.trumeet.mipushframework.component.SettingsRow
import top.trumeet.ui.theme.Layout

@Composable
fun ModuleSettings(onBack: () -> Unit) {
    val controller = LocalModuleController.current
    val scope = rememberCoroutineScope()
    val navigate = LocalPageNavigation.current
    ModuleSettingsLayout(R.string.module_settings, onBack) {
        ModuleStatusGroup()
        SettingsItem(title = stringResource(R.string.module_refresh)) {
            scope.launch { controller.read(refresh = true) }
        }
        SettingsGroup(title = stringResource(R.string.module_personalizations)) {
            personalizations.forEach { entry ->
                val state = controller.snapshot
                NavigationRow(
                    title = stringResource(entry.title),
                    summary = stringResource(
                        state?.let { entry.status(it).summary }
                            ?: controller.error ?: R.string.module_checking_summary
                    )
                ) { navigate(entry.route) }
            }
        }
    }
}

@Composable
fun QqNotificationSettings(onBack: () -> Unit) {
    val controller = LocalModuleController.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val saveError = stringResource(R.string.module_save_error)
    ModuleSettingsLayout(R.string.module_qq_settings, onBack, snackbar) {
        val state = controller.snapshot
        if (state == null) {
            SettingsRow(
                title = stringResource(if (controller.error == null) R.string.module_checking else R.string.module_not_ready),
                summary = stringResource(controller.error ?: R.string.module_checking_summary)
            )
            SettingsItem(title = stringResource(R.string.module_refresh)) {
                scope.launch { controller.read(refresh = true) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Layout.LineGap)) {
                SettingsGroup {
                    listOf(
                        Triple(Api.QQ_BADGE, R.string.module_qq_badge, R.string.module_qq_badge_summary),
                        Triple(Api.QQ_GEOMETRY, R.string.module_qq_geometry, R.string.module_qq_geometry_summary),
                        Triple(Api.QQ_SUMMARY, R.string.module_qq_summary, R.string.module_qq_summary_summary)
                    ).forEach { (key, title, summary) ->
                        val checked = state.getBoolean(key)
                        SettingsItem(
                            title = stringResource(title),
                            summary = stringResource(summary),
                            checked = checked,
                            enabled = !controller.saving
                        ) {
                            scope.launch {
                                if (!controller.set(key, !checked)) snackbar.showSnackbar(saveError)
                            }
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.module_qq_scope),
                    modifier = Modifier.padding(horizontal = Layout.CardPadding),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ModuleStatusGroup(showSettingsLink: Boolean = false) {
    val controller = LocalModuleController.current
    val state = controller.snapshot
    val navigate = LocalPageNavigation.current
    SettingsGroup(title = stringResource(R.string.module_status)) {
        if (state == null) {
            SettingsRow(
                title = stringResource(if (controller.error == null) R.string.module_checking else R.string.module_not_ready),
                summary = stringResource(controller.error ?: R.string.module_checking_summary)
            )
        } else {
            SettingsRow(
                title = stringResource(R.string.module_status_xmsf),
                summary = stringResource(when {
                    !state.getBoolean(Api.XMSF_LOADED) -> R.string.module_xmsf_missing
                    !state.getBoolean(Api.XMSF_CONFIG_READY) -> R.string.module_config_pending
                    else -> R.string.module_loaded
                })
            )
            SettingsRow(
                title = stringResource(R.string.module_status_system),
                summary = stringResource(if (state.getBoolean(Api.SYSTEM_READY)) R.string.module_ready else R.string.module_system_missing)
            )
        }
        SettingsRow(
            title = stringResource(R.string.module_personalizations),
            summary = personalizationSummary(state)
        )
        if (showSettingsLink) {
            NavigationRow(title = stringResource(R.string.module_settings)) { navigate(Pages.Module) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModuleSettingsLayout(
    @StringRes title: Int,
    onBack: () -> Unit,
    snackbar: SnackbarHostState? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                }
            )
        },
        snackbarHost = { snackbar?.let { SnackbarHost(it) } }
    ) { padding ->
        PageColumn(contentPadding = padding, content = content)
    }
}
