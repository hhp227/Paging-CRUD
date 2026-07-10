//
//  PostViewModel.swift
//  Paging-CRUD
//

import Foundation

@MainActor
final class PostViewModel: ObservableObject {
    @Published var state = State()

    private let repository: PostRepository

    init(repository: PostRepository = .shared) {
        self.repository = repository

        fetchPostList()
    }

    private func fetchPostList() {
        Task {
            for await result in repository.getPostList(groupId: Self.groupId, offset: 0) {
                switch result {
                case .success(let data):
                    state.isLoading = false
                    state.itemList = data
                case .error(let message, _):
                    state.isLoading = false
                    state.message = message
                case .loading:
                    state.isLoading = true
                }
            }
        }
    }

    func onDeletePost(_ post: ListItem.Post) {
        Task {
            for await result in repository.removePost(apiKey: URLs.apiKey, postId: post.id) {
                switch result {
                case .success:
                    state.isLoading = false
                    state.itemList.removeAll { $0.id == post.id }
                case .error(let message, _):
                    state.isLoading = false
                    state.message = message
                case .loading:
                    state.isLoading = true
                }
            }
        }
    }

    func refresh() {
        fetchPostList()
    }

    func onMessageShown() {
        state.message = ""
    }

    private static let groupId = 0

    struct State {
        var isLoading = false
        var itemList: [ListItem.Post] = []
        var message = ""
    }
}
