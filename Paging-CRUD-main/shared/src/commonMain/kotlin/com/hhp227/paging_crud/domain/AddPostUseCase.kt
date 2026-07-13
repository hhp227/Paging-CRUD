package com.hhp227.paging_crud.domain

import com.hhp227.paging_crud.data.PostRepository
import com.hhp227.paging_crud.model.Resource
import kotlinx.coroutines.flow.Flow

class AddPostUseCase(private val repository: PostRepository) {
    operator fun invoke(apiKey: String, groupId: Int, text: String): Flow<Resource<Int>> =
        repository.addPost(apiKey, groupId, text)
}
