//
//  KmpPagingBridgeAdapter.swift
//  iosApp
//
//  shared 프레임워크의 SwiftUiPagingBridge(androidx paging-common 엔진)를
//  Jetpack-Paging-for-SwiftUI의 PagingBridgeDataSource에 어댑팅한다.
//  (라이브러리 docs/KMP.md의 샘플 어댑터)
//

import Paging
import Shared

final class KmpPagingBridgeAdapter<T: AnyObject>: PagingBridgeDataSource {
    private let bridge: SwiftUiPagingBridge<T>

    var onPagesUpdated: (() -> Void)?

    var onLoadStatesUpdated: ((PagingBridgeCombinedLoadStates) -> Void)?

    init(_ bridge: SwiftUiPagingBridge<T>) {
        self.bridge = bridge
        bridge.onPagesUpdated = { [weak self] in self?.onPagesUpdated?() }
        bridge.onLoadStatesUpdated = { [weak self] states in
            self?.onLoadStatesUpdated?(states.toSwift())
        }
    }

    var count: Int { Int(bridge.count) }

    func item(at index: Int) -> Any? { bridge.item(index: Int32(index)) }

    func peekItem(at index: Int) -> Any? { bridge.peekItem(index: Int32(index)) }

    func refresh() { bridge.refresh() }

    func retry() { bridge.retry() }

    func start() { bridge.start() }

    deinit { bridge.dispose() }
}

private extension BridgeCombinedLoadStates {
    func toSwift() -> PagingBridgeCombinedLoadStates {
        PagingBridgeCombinedLoadStates(
            refresh: refresh.toSwift(),
            prepend: prepend.toSwift(),
            append: append.toSwift()
        )
    }
}

private extension BridgeLoadState {
    func toSwift() -> PagingBridgeLoadState {
        PagingBridgeLoadState(
            isLoading: isLoading,
            errorMessage: errorMessage,
            endOfPaginationReached: endOfPaginationReached
        )
    }
}
