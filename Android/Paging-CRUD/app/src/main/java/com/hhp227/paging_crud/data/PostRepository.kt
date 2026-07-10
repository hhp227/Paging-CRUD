package com.hhp227.paging_crud.data

import com.hhp227.paging_crud.api.PostService
import com.hhp227.paging_crud.model.ListItem
import com.hhp227.paging_crud.model.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart

class PostRepository(private val postService: PostService) {
    // TODO 스크롤이 하단에 도달시 offset으로 다음페이지를 불러오는 로직은 추후 구현
    fun getPostList(groupId: Int, offset: Int): Flow<Resource<out List<ListItem.Post>>> = flow {
        try {
            val response = postService.getPostList(groupId, offset, LOAD_SIZE)

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

        @Volatile private var instance: PostRepository? = null

        fun getInstance(postService: PostService) =
            instance ?: synchronized(this) {
                instance ?: PostRepository(postService).also { instance = it }
            }
    }
}
