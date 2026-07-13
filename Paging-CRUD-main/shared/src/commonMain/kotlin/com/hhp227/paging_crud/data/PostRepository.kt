package com.hhp227.paging_crud.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.hhp227.paging_crud.api.PostService
import com.hhp227.paging_crud.model.ListItem
import com.hhp227.paging_crud.model.Resource
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart

class PostRepository(
    private val postService: PostService,
    private val localDataSource: PostDao
) {
    @OptIn(ExperimentalPagingApi::class)
    fun getPostList(groupId: Int): Flow<PagingData<ListItem.Post>> {
        return Pager(
            // prefetchDistance = 1: 스크롤이 실제 바닥에 닿을 때만 다음 페이지 로드
            config = PagingConfig(enablePlaceholders = false, pageSize = LOAD_SIZE, initialLoadSize = LOAD_SIZE, prefetchDistance = 1),
            remoteMediator = PostRemoteMediator(postService, localDataSource, groupId),
            pagingSourceFactory = { PostLocalPagingSource(localDataSource, groupId) },
        ).flow
    }

    fun addPost(apiKey: String, groupId: Int, text: String): Flow<Resource<Int>> = flow {
        try {
            val response = postService.addPost(apiKey, text, groupId)

            if (!response.error) {
                requireNotNull(response.data)
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.message!!))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unexpected error occured"))
        }
    }
        .onStart { emit(Resource.Loading()) }

    fun removePost(apiKey: String, postId: Int): Flow<Resource<Boolean>> = flow {
        try {
            val response = postService.removePost(apiKey, postId)

            if (!response.error) {
                localDataSource.deletePost(postId)
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error(response.message!!, false))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unexpected error occured"))
        }
    }
        .onStart { emit(Resource.Loading()) }

    companion object {
        const val LOAD_SIZE = 10

        private val lock = SynchronizedObject()

        private var instance: PostRepository? = null

        fun getInstance(postService: PostService, postDao: PostDao) =
            instance ?: synchronized(lock) {
                instance ?: PostRepository(postService, postDao).also { instance = it }
            }
    }
}
