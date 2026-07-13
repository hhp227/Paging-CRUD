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
    // pagingData.collectAsLazyPagingItems()를 @StateObject로 직접 든다
    let pagingData: SwiftUiPagingBridge<ListItem.Post>

    private let removePostUseCase: RemovePostUseCase

    private let viewModelScope = ViewModelScope()

    init(
        getPostListUseCase: GetPostListUseCase = InjectorUtils.shared.provideGetPostListUseCase(),
        removePostUseCase: RemovePostUseCase = InjectorUtils.shared.provideRemovePostUseCase()
    ) {
        self.removePostUseCase = removePostUseCase
        self.pagingData = getPostListUseCase(groupId: Self.GROUP_ID).cachedIn(viewModelScope)
    }

    func onDeletePost(_ post: ListItem.Post) {
        removePostUseCase(apiKey: URLs.API_KEY, postId: post.id)
            .onEach { [weak self] result in
                switch result {
                case .success:
                    self?.state.isLoading = false
                case .error(let message, _):
                    self?.state.isLoading = false
                    self?.state.message = message
                case .loading:
                    self?.state.isLoading = true
                }
            }
            .launchIn(viewModelScope)
    }

    func onMessageShown() {
        state.message = ""
    }

    deinit {
        viewModelScope.cancel()
    }

    private static let GROUP_ID: Int32 = 0

    struct State {
        var isLoading = false
        var message = ""
    }
}
