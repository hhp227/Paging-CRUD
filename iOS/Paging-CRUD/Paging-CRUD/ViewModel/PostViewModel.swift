//
//  PostViewModel.swift
//  Paging-CRUD
//

import Foundation
import Combine
import Paging

@MainActor
final class PostViewModel: ObservableObject {
    @Published var state = State()

    let pagingData: AnyPublisher<PagingData<ListItem.Post>, Never>

    private let repository: PostRepository

    init(repository: PostRepository = .shared) {
        self.repository = repository
        self.pagingData = repository.getPostList(groupId: Self.groupId).cachedIn()
    }

    func onDeletePost(_ post: ListItem.Post) {
        Task {
            for await result in repository.removePost(apiKey: URLs.apiKey, postId: post.id) {
                switch result {
                case .success:
                    state.isLoading = false
                case .error(let message, _):
                    state.isLoading = false
                    state.message = message
                case .loading:
                    state.isLoading = true
                }
            }
        }
    }

    func onMessageShown() {
        state.message = ""
    }

    private static let groupId = 0

    struct State {
        var isLoading = false
        var message = ""
    }
}
