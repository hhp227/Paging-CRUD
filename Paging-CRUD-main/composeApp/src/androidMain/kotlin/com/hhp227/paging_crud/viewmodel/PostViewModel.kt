package com.hhp227.paging_crud.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.hhp227.paging_crud.data.PostRepository
import com.hhp227.paging_crud.model.ListItem
import com.hhp227.paging_crud.model.Resource
import com.hhp227.paging_crud.util.URLs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class PostViewModel internal constructor(
    private val repository: PostRepository
) : ViewModel() {
    val state = MutableStateFlow(State())

    private fun setPagingData(pagingData: PagingData<ListItem.Post>) {
        state.value = state.value.copy(pagingData = pagingData)
    }

    fun onDeletePost(post: ListItem.Post) {
        repository.removePost(URLs.API_KEY, post.id)
            .onEach { result ->
                when (result) {
                    is Resource.Success -> {
                        state.value = state.value.copy(isLoading = false)
                    }
                    is Resource.Error -> {
                        state.value = state.value.copy(
                            isLoading = false,
                            message = result.message ?: "An unexpected error occured"
                        )
                    }
                    is Resource.Loading -> {
                        state.value = state.value.copy(isLoading = true)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onMessageShown() {
        state.value = state.value.copy(message = "")
    }

    init {
        repository.getPostList(GROUP_ID)
            .cachedIn(viewModelScope)
            .catch { state.value = state.value.copy(message = it.message ?: "An unexpected error occured") }
            .onEach(::setPagingData)
            .launchIn(viewModelScope)
    }

    companion object {
        private const val GROUP_ID = 0
    }

    data class State(
        val isLoading: Boolean = false,
        val pagingData: PagingData<ListItem.Post> = PagingData.empty(),
        val message: String = ""
    )
}
