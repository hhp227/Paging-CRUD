//
//  CreatePostViewModel.swift
//  Paging-CRUD
//

import Foundation

@MainActor
final class CreatePostViewModel: ObservableObject {
    @Published var state = State()

    private let repository: PostRepository

    init(repository: PostRepository = .shared) {
        self.repository = repository
    }

    private func insertPost(text: String) {
        Task {
            for await result in repository.addPost(apiKey: URLs.apiKey, groupId: Self.groupId, text: text) {
                switch result {
                case .success(let data):
                    state.textError = nil
                    state.isLoading = false
                    state.postId = data
                case .error(let message, _):
                    state.textError = nil
                    state.isLoading = false
                    state.message = message
                case .loading:
                    state.textError = nil
                    state.isLoading = true
                }
            }
        }
    }

    func actionSend() {
        if !state.text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            insertPost(text: state.text)
        } else {
            state.textError = "내용을 입력해주세요."
        }
    }

    func onMessageShown() {
        state.message = ""
    }

    private static let groupId = 0

    struct State {
        var text = ""
        var textError: String? = nil
        var isLoading = false
        var postId = -1
        var message = ""
    }
}
