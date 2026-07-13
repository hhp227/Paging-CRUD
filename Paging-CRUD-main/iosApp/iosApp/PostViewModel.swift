//
//  PostViewModel.swift
//  iosApp
//

import Combine
import Foundation
import Shared

final class PostViewModel: ObservableObject {
    @Published var state = State()

    private let removePostUseCase: RemovePostUseCase

    private var cancellables = Set<AnyCancellable>()

    private func setPagingData(_ pagingData: PagingData<ListItem.Post>) {
        state.pagingData = pagingData
    }

    func onDeletePost(_ post: ListItem.Post) {
        removePostUseCase(apiKey: URLs.API_KEY, postId: post.id)
            .sink { [weak self] result in
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
            .store(in: &cancellables)
    }

    func onMessageShown() {
        state.message = ""
    }

    init(
        getPostListUseCase: GetPostListUseCase = InjectorUtils.shared.provideGetPostListUseCase(),
        removePostUseCase: RemovePostUseCase = InjectorUtils.shared.provideRemovePostUseCase()
    ) {
        self.removePostUseCase = removePostUseCase

        getPostListUseCase(groupId: Self.GROUP_ID)
            .cachedIn()
            .sink { [weak self] in self?.setPagingData($0) }
            .store(in: &cancellables)
    }

    private static let GROUP_ID: Int32 = 0

    struct State {
        var isLoading = false
        var pagingData: PagingData<ListItem.Post> = .empty()
        var message = ""
    }
}
