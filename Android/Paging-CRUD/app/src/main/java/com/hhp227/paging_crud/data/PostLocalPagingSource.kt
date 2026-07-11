package com.hhp227.paging_crud.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.hhp227.paging_crud.model.ListItem
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

class PostLocalPagingSource(
    private val postDao: PostDao,
    private val groupId: Int
) : PagingSource<Int, ListItem.Post>() {
    private val invalidationListener = { invalidate() }

    init {
        postDao.addInvalidationListener(invalidationListener)
        registerInvalidatedCallback { postDao.removeInvalidationListener(invalidationListener) }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ListItem.Post> {
        // 하단 도달시 로딩 인디케이터가 1초 보인 뒤 다음 페이지가 나타나도록 의도적으로 지연
        if (params is LoadParams.Append) {
            delay(1000)
        }
        val key = params.key ?: 0
        val count = postDao.getCount(groupId)
        val offset = when (params) {
            is LoadParams.Prepend -> max(0, key - params.loadSize)
            is LoadParams.Refresh -> if (key >= count) max(0, count - params.loadSize) else key
            else -> key
        }
        val limit = when (params) {
            is LoadParams.Prepend -> min(key, params.loadSize)
            else -> params.loadSize
        }
        val data = postDao.getPostList(groupId, offset, offset + limit)
        return LoadResult.Page(
            data = data,
            prevKey = if (offset <= 0 || data.isEmpty()) null else offset,
            nextKey = if (data.isEmpty() || offset + data.size >= count) null else offset + data.size
        )
    }

    override fun getRefreshKey(state: PagingState<Int, ListItem.Post>): Int? {
        // enablePlaceholders = false라 anchorPosition은 DAO 오프셋이 아닌 표시 인덱스이므로 페이지 기준으로 계산한다.
        // 앵커(마지막 접근 = 뷰포트 최하단)보다 한 페이지 위에서 시작해 refresh 윈도우(3페이지)가
        // 뷰포트 전체를 덮게 해서, 무효화 후에도 보이는 아이템들의 key가 유지되어 스크롤이 밀리지 않는다
        return state.anchorPosition?.let { anchorPosition ->
            val pageStart = state.closestPageToPosition(anchorPosition)?.prevKey ?: 0

            max(0, pageStart - state.config.pageSize)
        }
    }
}
