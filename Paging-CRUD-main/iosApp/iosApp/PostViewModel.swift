//
//  PostViewModel.swift
//  iosApp
//

import Foundation
import Shared

final class PostViewModel: ObservableObject {
    @Published var state = State()

    // LazyPagingItems는 ObservableObject라서 ViewModel 프로퍼티로 중첩하면 View가 변경을
    // 관찰하지 못한다. ViewModel은 브리지만 노출하고 View가
    // pagingData.collectAsLazyPagingItems()를 @StateObject로 직접 든다 (pure-Swift 모드와 동일한 표면)
    let pagingData: SwiftUiPagingBridge<ListItem.Post>

    private let removePostBridge: RemovePostBridge

    init(
        getPostListUseCase: GetPostListUseCase = InjectorUtils.shared.provideGetPostListUseCase(),
        removePostUseCase: RemovePostUseCase = InjectorUtils.shared.provideRemovePostUseCase()
    ) {
        self.pagingData = getPostListUseCase.asBridge(groupId: Self.groupId)
        self.removePostBridge = RemovePostBridge(removePostUseCase: removePostUseCase)
    }

    func onDeletePost(_ post: ListItem.Post) {
        removePostBridge.removePost(postId: post.id) { [weak self] result in
            DispatchQueue.main.async {
                guard let self = self else { return }

                self.state.isLoading = result.isLoading

                if let message = result.errorMessage {
                    self.state.message = message
                }
            }
        }
    }

    func onMessageShown() {
        state.message = ""
    }

    deinit {
        removePostBridge.dispose()
    }

    private static let groupId: Int32 = 0

    struct State {
        var isLoading = false
        var message = ""
    }
}
