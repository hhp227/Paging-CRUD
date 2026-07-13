// Jetpack-Paging-for-SwiftUI kmp-module 브랜치의 kmp/paging-swiftui 모듈에서 벤더링한 파일.
// 라이브러리가 Maven에 배포되면 io.github.hhp227:paging-swiftui 의존성으로 교체하고 이 파일은 삭제한다.

package io.github.hhp227.paging.swiftui

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState

/**
 * Kotlin의 sealed class [LoadState]를 ObjC 경계 너머로 옮기기 위한 평탄화 표현.
 * Swift 쪽 PagingBridgeLoadState와 1:1 대응한다.
 */
data class BridgeLoadState(
    val isLoading: Boolean,
    val errorMessage: String?,
    val endOfPaginationReached: Boolean,
)

data class BridgeCombinedLoadStates(
    val refresh: BridgeLoadState,
    val prepend: BridgeLoadState,
    val append: BridgeLoadState,
)

internal fun CombinedLoadStates.toBridge(): BridgeCombinedLoadStates = BridgeCombinedLoadStates(
    refresh = refresh.toBridge(),
    prepend = prepend.toBridge(),
    append = append.toBridge(),
)

internal fun LoadState.toBridge(): BridgeLoadState = BridgeLoadState(
    isLoading = this is LoadState.Loading,
    errorMessage = (this as? LoadState.Error)?.error?.let { it.message ?: it.toString() },
    endOfPaginationReached = endOfPaginationReached,
)
