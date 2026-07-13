package com.hhp227.paging_crud.bridge

import androidx.paging.PagingData
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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// Kotlin Flow ↔ Combine Publisher 대응 계층.
// Swift 쪽(KmpInterop.swift)이 이 핸들들을 Combine Publisher로 감싸서,
// SwiftUI ViewModel이 Compose ViewModel과 1:1 코드 패턴을 갖게 한다.

/** Combine Subscription이 cancel을 위임하는 구독 핸들 */
class FlowSubscription internal constructor(private val scope: CoroutineScope) {
    fun cancel() {
        scope.cancel()
    }
}

/** Flow<T>를 Combine 퍼블리셔로 옮기기 위한 콜드 핸들. 구독마다 수집 코루틴이 새로 시작된다 */
class FlowAdapter<T : Any> internal constructor(private val source: Flow<T>) {
    fun subscribe(onEach: (T) -> Unit): FlowSubscription {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        scope.launch { source.collect { onEach(it) } }
        return FlowSubscription(scope)
    }
}

/**
 * getPostList의 Flow<PagingData> 대응 핸들. cachedIn()은 Kotlin의 cachedIn(viewModelScope)
 * 대응으로, 실제 캐시는 구독 시점에 만들어지는 구독 수명 스코프에 적용된다
 * (Swift ViewModel의 cancellables 수명 == viewModelScope 수명).
 */
class PostPagingFlowAdapter internal constructor(
    private val source: Flow<PagingData<ListItem.Post>>,
    private val cached: Boolean = false
) {
    fun cachedIn(): PostPagingFlowAdapter = PostPagingFlowAdapter(source, cached = true)

    fun subscribe(onEach: (PagingData<ListItem.Post>) -> Unit): FlowSubscription {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val flow = if (cached) source.cachedIn(scope) else source

        scope.launch { flow.collect { onEach(it) } }
        return FlowSubscription(scope)
    }
}

/** Kotlin의 getPostListUseCase(groupId) 호출 대응 — Swift callAsFunction이 감싼다 */
fun GetPostListUseCase.pagingFlow(groupId: Int): PostPagingFlowAdapter =
    PostPagingFlowAdapter(invoke(groupId))

/** 게시글 작성/삭제 결과를 ObjC 경계 너머로 옮기기 위한 평탄화 표현 (Swift에서 Resource enum으로 복원) */
class PostOpResult(
    val isLoading: Boolean,
    val errorMessage: String?,
    /** addPost 성공시 새 글 id, 그 외 -1 */
    val postId: Int
)

fun AddPostUseCase.opFlow(apiKey: String, groupId: Int, text: String): FlowAdapter<PostOpResult> =
    FlowAdapter(invoke(apiKey, groupId, text).map { it.toOpResult { data -> data ?: -1 } })

fun RemovePostUseCase.opFlow(apiKey: String, postId: Int): FlowAdapter<PostOpResult> =
    FlowAdapter(invoke(apiKey, postId).map { it.toOpResult { -1 } })

/**
 * Swift의 Publisher.collectAsLazyPagingItems()가 사용하는 push형 브리지.
 * State에서 흘러나온 PagingData(Combine 퍼블리셔)를 presenter(SwiftUiPagingBridge)로 전달한다.
 */
class PagingDataSubject<T : Any> {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val pagingDataFlow = MutableSharedFlow<PagingData<T>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val bridge: SwiftUiPagingBridge<T> = SwiftUiPagingBridge(pagingDataFlow, scope)

    fun send(pagingData: PagingData<T>) {
        pagingDataFlow.tryEmit(pagingData)
    }
}

/** Swift State 기본값용 — Kotlin의 PagingData.empty() 대응 */
fun emptyPostPagingData(): PagingData<ListItem.Post> = PagingData.empty()

private fun <T> Resource<T>.toOpResult(postId: (T?) -> Int): PostOpResult = when (this) {
    is Resource.Success -> PostOpResult(isLoading = false, errorMessage = null, postId = postId(data))
    is Resource.Error -> PostOpResult(isLoading = false, errorMessage = message ?: "An unexpected error occured", postId = -1)
    is Resource.Loading -> PostOpResult(isLoading = true, errorMessage = null, postId = -1)
}
