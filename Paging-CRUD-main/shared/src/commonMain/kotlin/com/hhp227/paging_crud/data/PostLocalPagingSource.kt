package com.hhp227.paging_crud.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.hhp227.paging_crud.model.ListItem
import kotlinx.coroutines.delay
import kotlin.math.min

// 표시 목록은 항상 캐시의 0번부터 시작하는 프리픽스라서 무효화가 뷰포트 위쪽 행을 건드리지 않고
// (스크롤 유지), prepend는 발생하지 않는다. 아래로는 APPEND가 페이지 단위로 이어 붙인다
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
        val count = postDao.getCount(groupId)
        val offset: Int
        val end: Int

        when (params) {
            is LoadParams.Prepend -> return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
            is LoadParams.Append -> {
                offset = params.key
                end = min(count, offset + params.loadSize)
            }
            else -> {
                offset = 0
                end = min(count, params.key ?: params.loadSize)
            }
        }
        val data = postDao.getPostList(groupId, offset, end)
        return LoadResult.Page(
            data = data,
            prevKey = null,
            nextKey = if (data.isEmpty() || end >= count) null else end
        )
    }

    override fun getRefreshKey(state: PagingState<Int, ListItem.Post>): Int? {
        // 무효화 전에 표시 중이던 프리픽스 전체를 그대로 다시 표시한다 (key = 표시 끝 오프셋)
        return state.pages.sumOf { it.data.size }.takeIf { it > 0 }
    }
}
