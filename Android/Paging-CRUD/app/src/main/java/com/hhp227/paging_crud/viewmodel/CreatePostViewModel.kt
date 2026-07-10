package com.hhp227.paging_crud.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.paging_crud.data.PostRepository
import com.hhp227.paging_crud.model.Resource
import com.hhp227.paging_crud.util.URLs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class CreatePostViewModel internal constructor(
    private val repository: PostRepository
) : ViewModel() {
    val state = MutableStateFlow(State())

    private fun insertPost(text: String) {
        repository.addPost(URLs.API_KEY, GROUP_ID, text)
            .onEach { result ->
                when (result) {
                    is Resource.Success -> {
                        state.value = state.value.copy(
                            textError = null,
                            isLoading = false,
                            postId = result.data ?: -1
                        )
                    }
                    is Resource.Error -> {
                        state.value = state.value.copy(
                            textError = null,
                            isLoading = false,
                            message = result.message ?: "An unexpected error occured"
                        )
                    }
                    is Resource.Loading -> {
                        state.value = state.value.copy(textError = null, isLoading = true)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onTextChange(text: String) {
        state.value = state.value.copy(text = text, textError = null)
    }

    fun actionSend() {
        if (state.value.text.isNotBlank()) {
            insertPost(state.value.text)
        } else {
            state.value = state.value.copy(textError = "내용을 입력해주세요.")
        }
    }

    fun onMessageShown() {
        state.value = state.value.copy(message = "")
    }

    companion object {
        private const val GROUP_ID = 0
    }

    data class State(
        val text: String = "",
        val textError: String? = null,
        val isLoading: Boolean = false,
        val postId: Int = -1,
        val message: String = ""
    )
}
