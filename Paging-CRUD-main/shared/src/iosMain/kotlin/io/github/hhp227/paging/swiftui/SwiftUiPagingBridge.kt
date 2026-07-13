// Jetpack-Paging-for-SwiftUI kmp-module 브랜치의 kmp/paging-swiftui 모듈에서 벤더링한 파일.
// 라이브러리가 Maven에 배포되면 io.github.hhp227:paging-swiftui 의존성으로 교체하고 이 파일은 삭제한다.

package io.github.hhp227.paging.swiftui

import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * androidx paging-common의 [PagingDataPresenter]를 Swift(SwiftUI)에서 다루기 좋은
 * 평탄한 API로 감싼 브리지.
 *
 * iOS 앱은 shared 프레임워크로 노출된 이 클래스를 Jetpack-Paging-for-SwiftUI(SPM)의
 * `PagingBridgeDataSource` 프로토콜에 어댑팅해서 `LazyPagingItems(bridge:)`로 주입한다.
 * 어댑터 샘플은 저장소의 docs/KMP.md 참고.
 *
 * 주의: Swift 쪽이 [onPagesUpdated]/[onLoadStatesUpdated]를 연결한 **뒤에** [start]를
 * 호출해야 초기 이벤트가 유실되지 않는다. 이 클래스는 [start] 전에는 아무것도 방출하지 않는다.
 */
class SwiftUiPagingBridge<T : Any>(
    private val pagingDataFlow: Flow<PagingData<T>>,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    /** 표시 목록이 갱신될 때마다 메인 스레드에서 호출된다 */
    var onPagesUpdated: (() -> Unit)? = null

    /** 로드 상태가 바뀔 때마다 호출된다 */
    var onLoadStatesUpdated: ((BridgeCombinedLoadStates) -> Unit)? = null

    private var started = false

    // paging-compose의 LazyPagingItems와 동일한 패턴: 내부 페이지 상태가 갱신된 직후
    // presentPagingDataEvent가 메인 컨텍스트에서 호출되므로, 이 시점의 size/peek는 최신 상태다
    private val presenter = object : PagingDataPresenter<T>(Dispatchers.Main) {
        override suspend fun presentPagingDataEvent(event: PagingDataEvent<T>) {
            onPagesUpdated?.invoke()
        }
    }

    /** 현재 표시 가능한 아이템 수 (presenter.size) */
    val count: Int get() = presenter.size

    /** PagingData 수집 시작. 콜백 연결이 끝난 뒤 한 번만 호출한다 (중복 호출은 무시) */
    fun start() {
        if (started) return
        started = true

        scope.launch {
            pagingDataFlow.collectLatest { presenter.collectFrom(it) }
        }
        scope.launch {
            presenter.loadStateFlow.filterNotNull().collect {
                onLoadStatesUpdated?.invoke(it.toBridge())
            }
        }
    }

    /** 아이템 반환 + 로드 힌트 트리거. 행이 화면에 나타날 때(onAppear) 메인 스레드에서 호출한다 */
    fun item(index: Int): T? = presenter[index]

    /** 힌트 없이 아이템만 반환 */
    fun peekItem(index: Int): T? = presenter.peek(index)

    /** 현재 표시 목록의 스냅샷 */
    fun snapshotItems(): List<T> = presenter.snapshot().items

    fun refresh() = presenter.refresh()

    fun retry() = presenter.retry()

    /** 수집 중단. Swift 어댑터의 deinit에서 호출한다 */
    fun dispose() {
        scope.cancel()
    }
}
