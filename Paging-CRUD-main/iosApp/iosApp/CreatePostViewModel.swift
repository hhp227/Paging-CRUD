//
//  CreatePostViewModel.swift
//  iosApp
//

import Combine
import Foundation
import Shared

final class CreatePostViewModel: ObservableObject {
    @Published var state = State()

    private let addPostUseCase: AddPostUseCase

    private var cancellables = Set<AnyCancellable>()

    init(addPostUseCase: AddPostUseCase = InjectorUtils.shared.provideAddPostUseCase()) {
        self.addPostUseCase = addPostUseCase
    }

    private func insertPost(text: String) {
        addPostUseCase(apiKey: URLs.API_KEY, groupId: Self.GROUP_ID, text: text)
            .sink { [weak self] result in
                switch result {
                case .success(let data):
                    self?.state.textError = nil
                    self?.state.isLoading = false
                    self?.state.postId = data
                case .error(let message, _):
                    self?.state.textError = nil
                    self?.state.isLoading = false
                    self?.state.message = message
                case .loading:
                    self?.state.textError = nil
                    self?.state.isLoading = true
                }
            }
            .store(in: &cancellables)
    }

    func onTextChange(_ text: String) {
        state.text = text
        state.textError = nil
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

    private static let GROUP_ID: Int32 = 0

    struct State {
        var text = ""
        var textError: String? = nil
        var isLoading = false
        var postId = -1
        var message = ""
    }
}
