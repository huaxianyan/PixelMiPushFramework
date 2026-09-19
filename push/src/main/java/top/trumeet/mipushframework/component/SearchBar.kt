package top.trumeet.mipushframework.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.xiaomi.xmsf.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.trumeet.ui.theme.Layout

/** A filled, rounded search field that lines up with the page grid. */
@Composable
fun SearchBar(placeholder: String, onValueChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    val ph by rememberUpdatedState(placeholder)
    val debounceOnValueChange: (String) -> Unit = debounce(onValueChange)
    val change: (String) -> Unit = { query = it; debounceOnValueChange(it) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        TextField(
            value = query,
            onValueChange = change,
            modifier = Modifier
                .widthIn(max = Layout.ContentMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = Layout.PageHorizontal, vertical = 8.dp),
            placeholder = { Text(ph) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton({ change("") }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(R.string.action_clear_search)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun debounce(onValueChange: (String) -> Unit): (String) -> Unit {
    val scope = rememberCoroutineScope()
    var job: Job? = null
    val change: (String) -> Unit = {
        scope.launch {
            job?.cancel()
            job = launch {
                delay(300)
                onValueChange(it)
            }
        }
    }
    return change
}
