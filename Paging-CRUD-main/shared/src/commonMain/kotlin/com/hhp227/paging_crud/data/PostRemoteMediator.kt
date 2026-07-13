package com.hhp227.paging_crud.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.hhp227.paging_crud.api.PostService
import com.hhp227.paging_crud.model.ListItem
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val postService: PostService,
    private val postDao: PostDao,
    private val groupId: Int
) : RemoteMediator<Int, ListItem.Post>() {
    override suspend fun load(loadType: LoadType, state: PagingState<Int, ListItem.Post>): MediatorResult {
        return try {
            val offset = when (loadType) {
                LoadType.REFRESH -> 0
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> postDao.getCount(groupId)
            }
            val loadSize = if (loadType == LoadType.REFRESH) state.config.initialLoadSize else state.config.pageSize
            val response = postService.getPostList(groupId, offset, loadSize)

            if (!response.error) {
                val data = response.data ?: emptyList()

                if (loadType == LoadType.REFRESH) {
                    postDao.replaceAll(groupId, data)
                } else {
                    postDao.insertAll(groupId, data)
                }
                MediatorResult.Success(endOfPaginationReached = data.isEmpty())
            } else {
                MediatorResult.Error(
                    Throwable(response.message)
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
