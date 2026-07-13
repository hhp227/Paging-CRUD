//
//  CreatePostViewModel.swift
//  iosApp
//

import Foundation
import Shared

final class CreatePostViewModel: ObservableObject {
    @Published var state = State()

    private let addPostUseCase: AddPostUseCase

    private let viewModelScope = ViewModelScope()

    init(addPostUseCase: AddPostUseCase = InjectorUtils.shared.provideAddPostUseCase()) {
        self.addPostUseCase = addPostUseCase
    }

    private func insertPost(text: String) {
        addPostUseCase(apiKey: URLs.API_KEY, groupId: Self.GROUP_ID, text: text)
            .onEach { [weak self] result in
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
            .launchIn(viewModelScope)
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

    deinit {
        viewModelScope.cancel()
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
