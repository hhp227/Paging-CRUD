package com.hhp227.paging_crud.bridge

import androidx.paging.cachedIn
import com.hhp227.paging_crud.domain.AddPostUseCase
import com.hhp227.paging_crud.domain.GetPostListUseCase
import com.hhp227.paging_crud.domain.RemovePostUseCase
import com.hhp227.paging_crud.model.ListItem
import com.hhp227.paging_crud.model.Resource
import io.github.hhp227.paging.swiftui.SwiftUiPagingBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * androidx viewModelScope의 iOS 대응물. Swift ViewModel이 프로퍼티로 소유하고
 * deinit에서 [cancel]을 호출한다 — Kotlin ViewModel의 onCleared와 같은 역할.
 * 이 스코프에 cachedIn 캐시와 CRUD Flow 수집이 모두 묶인다.
 */
class ViewModelScope {
    internal val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun cancel() {
        coroutineScope.cancel()
    }
}

/**
 * Compose ViewModel의 `getPostListUseCase(GROUP_ID).cachedIn(viewModelScope)`와
 * 동일한 호출 패턴을 만들기 위한 확장. Swift 쪽 callAsFunction/PostPagingFlow 셔거와
 * 조합되어 `getPostListUseCase(groupId:).cachedIn(viewModelScope)`로 읽힌다.
 */
fun GetPostListUseCase.cachedIn(groupId: Int, scope: ViewModelScope): SwiftUiPagingBridge<ListItem.Post> =
    SwiftUiPagingBridge(invoke(groupId).cachedIn(scope.coroutineScope), scope.coroutineScope)

/** 게시글 작성/삭제 결과를 ObjC 경계 너머로 옮기기 위한 평탄화 표현 (Swift에서 Resource enum으로 복원) */
class PostOpResult(
    val isLoading: Boolean,
    val errorMessage: String?,
    /** addPost 성공시 새 글 id, 그 외 -1 */
    val postId: Int
)

/**
 * Kotlin의 `.onEach { }.launchIn(viewModelScope)` 패턴을 Swift에서 재현하기 위한
 * 수집 진입점. Swift 쪽 ResourceFlow 셔거가 launchIn 시점에 호출한다.
 * 콜백은 메인 디스패처에서 호출된다.
 */
fun AddPostUseCase.collectIn(
    scope: ViewModelScope,
    apiKey: String,
    groupId: Int,
    text: String,
    onEach: (PostOpResult) -> Unit
) {
    scope.coroutineScope.launch {
        invoke(apiKey, groupId, text).collect { result ->
            onEach(result.toOpResult { it ?: -1 })
        }
    }
}

fun RemovePostUseCase.collectIn(
    scope: ViewModelScope,
    apiKey: String,
    postId: Int,
    onEach: (PostOpResult) -> Unit
) {
    scope.coroutineScope.launch {
        invoke(apiKey, postId).collect { result ->
            onEach(result.toOpResult { -1 })
        }
    }
}

private fun <T> Resource<T>.toOpResult(postId: (T?) -> Int): PostOpResult = when (this) {
    is Resource.Success -> PostOpResult(isLoading = false, errorMessage = null, postId = postId(data))
    is Resource.Error -> PostOpResult(isLoading = false, errorMessage = message ?: "An unexpected error occured", postId = -1)
    is Resource.Loading -> PostOpResult(isLoading = true, errorMessage = null, postId = -1)
}
