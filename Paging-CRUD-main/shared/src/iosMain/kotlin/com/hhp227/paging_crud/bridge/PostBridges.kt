package com.hhp227.paging_crud.bridge

import androidx.paging.cachedIn
import com.hhp227.paging_crud.model.ListItem
import com.hhp227.paging_crud.model.Resource
import com.hhp227.paging_crud.util.InjectorUtils
import com.hhp227.paging_crud.util.URLs
import io.github.hhp227.paging.swiftui.SwiftUiPagingBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * iOS(SwiftUI) 쪽 페이징 진입점. Swift ViewModel에는 Kotlin scope가 없으므로 브리지가
 * scope를 소유하고 cachedIn도 이 경계에서 적용한다 (Android는 ViewModel의 viewModelScope).
 * scope의 수명은 Swift 어댑터의 deinit → dispose()가 정리한다.
 */
fun postPagingBridge(groupId: Int): SwiftUiPagingBridge<ListItem.Post> {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    return SwiftUiPagingBridge(
        InjectorUtils.getPostRepository().getPostList(groupId).cachedIn(scope),
        scope
    )
}

/** 게시글 작성/삭제 결과를 ObjC 경계 너머로 옮기기 위한 평탄화 표현 */
class PostOpResult(
    val isLoading: Boolean,
    val errorMessage: String?,
    /** addPost 성공시 새 글 id, 그 외 -1 */
    val postId: Int
)

/** Swift ViewModel이 소비하는 게시글 작성/삭제 브리지. 콜백은 메인 디스패처에서 호출된다 */
class PostCrudBridge(private val groupId: Int) {
    private val repository = InjectorUtils.getPostRepository()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun addPost(text: String, onEach: (PostOpResult) -> Unit) {
        scope.launch {
            repository.addPost(URLs.API_KEY, groupId, text).collect { result ->
                onEach(result.toOpResult { it ?: -1 })
            }
        }
    }

    fun removePost(postId: Int, onEach: (PostOpResult) -> Unit) {
        scope.launch {
            repository.removePost(URLs.API_KEY, postId).collect { result ->
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
