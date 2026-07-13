package com.hhp227.paging_crud.bridge

import androidx.paging.cachedIn
import com.hhp227.paging_crud.domain.AddPostUseCase
import com.hhp227.paging_crud.domain.GetPostListUseCase
import com.hhp227.paging_crud.domain.RemovePostUseCase
import com.hhp227.paging_crud.model.ListItem
import com.hhp227.paging_crud.model.Resource
import com.hhp227.paging_crud.util.URLs
import io.github.hhp227.paging.swiftui.SwiftUiPagingBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * iOS(SwiftUI) 쪽 페이징 진입점 (docs/KMP.md의 계층 배치).
 * UseCase는 cachedIn 없는 Flow<PagingData>를 반환하고, Swift ViewModel에는 Kotlin
 * scope가 없으므로 이 경계에서 브리지가 scope를 소유하며 cachedIn을 적용한다
 * (Android는 ViewModel의 viewModelScope에서 적용). scope의 수명은 Swift 어댑터의
 * deinit → dispose()가 정리한다.
 */
fun GetPostListUseCase.asBridge(groupId: Int): SwiftUiPagingBridge<ListItem.Post> {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    return SwiftUiPagingBridge(invoke(groupId).cachedIn(scope), scope)
}

/** 게시글 작성/삭제 결과를 ObjC 경계 너머로 옮기기 위한 평탄화 표현 */
class PostOpResult(
    val isLoading: Boolean,
    val errorMessage: String?,
    /** addPost 성공시 새 글 id, 그 외 -1 */
    val postId: Int
)

/** AddPostUseCase를 Swift 콜백으로 노출하는 브리지. 콜백은 메인 디스패처에서 호출된다 */
class AddPostBridge(
    private val addPostUseCase: AddPostUseCase,
    private val groupId: Int
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun addPost(text: String, onEach: (PostOpResult) -> Unit) {
        scope.launch {
            addPostUseCase(URLs.API_KEY, groupId, text).collect { result ->
                onEach(result.toOpResult { it ?: -1 })
            }
        }
    }

    fun dispose() {
        scope.cancel()
    }
}

/** RemovePostUseCase를 Swift 콜백으로 노출하는 브리지. 콜백은 메인 디스패처에서 호출된다 */
class RemovePostBridge(
    private val removePostUseCase: RemovePostUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun removePost(postId: Int, onEach: (PostOpResult) -> Unit) {
        scope.launch {
            removePostUseCase(URLs.API_KEY, postId).collect { result ->
                onEach(result.toOpResult { -1 })
            }
        }
    }

    fun dispose() {
        scope.cancel()
    }
}

private fun <T> Resource<T>.toOpResult(postId: (T?) -> Int): PostOpResult = when (this) {
    is Resource.Success -> PostOpResult(isLoading = false, errorMessage = null, postId = postId(data))
    is Resource.Error -> PostOpResult(isLoading = false, errorMessage = message ?: "An unexpected error occured", postId = -1)
    is Resource.Loading -> PostOpResult(isLoading = true, errorMessage = null, postId = -1)
}
