//
//  PostViewModel.swift
//  iosApp
//

import Foundation
import Paging
import Shared

final class PostViewModel: ObservableObject {
    @Published var state = State()

    private let getPostListUseCase: GetPostListUseCase

    private let removePostBridge: RemovePostBridge

    init(
        getPostListUseCase: GetPostListUseCase = InjectorUtils.shared.provideGetPostListUseCase(),
        removePostUseCase: RemovePostUseCase = InjectorUtils.shared.provideRemovePostUseCase()
    ) {
        self.getPostListUseCase = getPostListUseCase
        self.removePostBridge = RemovePostBridge(removePostUseCase: removePostUseCase)
    }

    // LazyPagingItems는 ObservableObject라서 ViewModel 프로퍼티로 중첩하면 View가 변경을
    // 관찰하지 못한다. ViewModel은 팩토리만 제공하고 View가 @StateObject로 직접 든다
    func makePagingItems() -> LazyPagingItems<ListItem.Post> {
        LazyPagingItems(bridge: KmpPagingBridgeAdapter(getPostListUseCase.asBridge(groupId: Self.groupId)))
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
