package com.hhp227.paging_crud.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.hhp227.paging_crud.model.ListItem
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
        // enablePlaceholders = false라 anchorPosition은 DAO 오프셋이 아닌 표시 인덱스이므로,
        // 앵커가 속한 페이지의 시작 오프셋(prevKey)으로 페이지 정렬해서 리프레시한다
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }
}
