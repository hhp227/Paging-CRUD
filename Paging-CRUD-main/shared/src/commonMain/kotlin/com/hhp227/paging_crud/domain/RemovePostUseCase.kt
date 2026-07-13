package com.hhp227.paging_crud.domain

import com.hhp227.paging_crud.data.PostRepository
import com.hhp227.paging_crud.model.Resource
import kotlinx.coroutines.flow.Flow

class RemovePostUseCase(private val repository: PostRepository) {
    operator fun invoke(apiKey: String, postId: Int): Flow<Resource<Boolean>> =
        repository.removePost(apiKey, postId)
}
