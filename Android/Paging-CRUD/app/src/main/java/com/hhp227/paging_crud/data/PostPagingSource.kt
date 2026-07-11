package com.hhp227.paging_crud.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.hhp227.paging_crud.api.PostService
import com.hhp227.paging_crud.model.ListItem
import retrofit2.HttpException
import java.io.IOException
import kotlin.math.max

class PostPagingSource(
    private val postService: PostService,
    private val postDao: PostDao,
    private val groupId: Int
) : PagingSource<Int, ListItem.Post>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ListItem.Post> {
        return try {
            val offset: Int = params.key ?: 0
            val loadSize: Int = params.loadSize
            val key = max(0, offset + postDao.getCount(groupId))
            val nextKey = key + loadSize
            val prevKey = key - loadSize
            val response = postService.getPostList(groupId, key, loadSize)

            if (offset == 0) postDao.deleteAll(groupId)
            if (!response.error) {
                val data = response.data ?: emptyList()

                postDao.insertAll(groupId, data)
                LoadResult.Page(
                    data = postDao.getPostList(groupId, key, nextKey),
                    prevKey = if (offset == 0) null else prevKey,
                    nextKey = if (data.isEmpty()) null else nextKey
                )
            } else {
                LoadResult.Error(
                    Throwable(response.message)
                )
            }
        } catch (e: IOException) {
            LoadResult.Error(e)
        } catch (e: HttpException) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ListItem.Post>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            if (postDao.isCacheEmpty(groupId)) null
            else state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }
}
