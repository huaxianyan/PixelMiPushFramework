package top.trumeet.mipushframework.main.subpage

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Keeps the existing descending-ID query and decodes only the requested batch off the UI thread. */
internal class EventHistoryPagingSource(
    private val readEvents: (beforeId: Long?, size: Int) -> List<EventInfoForDisplay>
) : PagingSource<Long, EventInfoForDisplay>() {
    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, EventInfoForDisplay> =
        try {
            val events = withContext(Dispatchers.IO) { readEvents(params.key, params.loadSize) }
            LoadResult.Page(
                data = events,
                prevKey = null,
                nextKey = if (events.size < params.loadSize) null else events.last().id
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            LoadResult.Error(error)
        }

    // A refreshed history starts with the newest records, not the previously visible older page.
    override fun getRefreshKey(state: PagingState<Long, EventInfoForDisplay>): Long? = null
}
