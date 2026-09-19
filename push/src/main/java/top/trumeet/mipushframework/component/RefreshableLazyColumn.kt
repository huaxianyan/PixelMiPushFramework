package top.trumeet.mipushframework.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import top.trumeet.ui.theme.Layout

/**
 * A pull-to-refresh list that already follows the shared page rhythm.
 *
 * Callers only describe their rows; the edge padding, the gap between rows and the centred
 * maximum width come from here, so every list in the app lines up with every other page.
 */
@Composable
fun RefreshableLazyColumn(
    doRefresh: (onRefreshed: () -> Unit) -> Unit,
    isNeedMore: (lastVisibleIndex: Int) -> Boolean,
    doLoadMore: (onRefreshed: () -> Unit) -> Unit,
    isNeedRefresh: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = Layout.PageHorizontal,
        vertical = Layout.PageVertical
    ),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(Layout.ListGap),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: LazyListScope.() -> Unit
) {
    val currentIsNeedMore by rememberUpdatedState(isNeedMore)
    val currentDoLoadMore by rememberUpdatedState(doLoadMore)

    var isRefreshing by remember { mutableStateOf(false) }
    val onRefreshed by remember { mutableStateOf({ isRefreshing = false }) }

    if (isNeedRefresh) {
        isRefreshing = true
        SideEffect {
            doRefresh(onRefreshed)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing),
            onRefresh = {
                isRefreshing = true
                doRefresh(onRefreshed)
            },
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = Layout.ContentMaxWidth)
                .fillMaxWidth()
        ) {
            val lazyListState = rememberLazyListState()
            LaunchedEffect(lazyListState) {
                snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo }
                    .collect { visibleItems ->
                        if (isRefreshing) return@collect
                        val lastIndex = if (visibleItems.isNotEmpty())
                            visibleItems.last().index else 0
                        if (currentIsNeedMore(lastIndex)) {
                            isRefreshing = true
                            currentDoLoadMore(onRefreshed)
                        }
                    }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = contentPadding,
                verticalArrangement = verticalArrangement,
                horizontalAlignment = horizontalAlignment,
                content = content
            )
        }
    }
}
