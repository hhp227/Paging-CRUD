package com.hhp227.paging_crud.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.paging_crud.data.PostRepository
import com.hhp227.paging_crud.model.ListItem
import com.hhp227.paging_crud.model.Resource
import com.hhp227.paging_crud.util.URLs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class PostViewModel internal constructor(
    private val repository: PostRepository
) : ViewModel() {
    val state = MutableStateFlow(State())

    private fun fetchPostList() {
        repository.getPostList(GROUP_ID, 0)
            .onEach { result ->
                when (result) {
                    is Resource.Success -> {
                        state.value = state.value.copy(
                            isLoading = false,
                            itemList = result.data ?: emptyList()
                        )
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

    fun onDeletePost(post: ListItem.Post) {
        repository.removePost(URLs.API_KEY, post.id)
            .onEach { result ->
                when (result) {
                    is Resource.Success -> {
                        state.value = state.value.copy(
                            isLoading = false,
                            itemList = state.value.itemList.filter { it.id != post.id }
                        )
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

    fun refresh() {
        fetchPostList()
    }

    fun onMessageShown() {
        state.value = state.value.copy(message = "")
    }

    init {
        fetchPostList()
    }

    companion object {
        private const val GROUP_ID = 0
    }

    data class State(
        val isLoading: Boolean = false,
        val itemList: List<ListItem.Post> = emptyList(),
        val message: String = ""
    )
}
