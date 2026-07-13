//
//  CreatePostViewModel.swift
//  iosApp
//

import Foundation
import Shared

final class CreatePostViewModel: ObservableObject {
    @Published var state = State()

    private let crudBridge = PostCrudBridge(groupId: Self.groupId)

    private func insertPost(text: String) {
        crudBridge.addPost(text: text) { [weak self] result in
            DispatchQueue.main.async {
                guard let self = self else { return }

                self.state.textError = nil
                self.state.isLoading = result.isLoading

                if let message = result.errorMessage {
                    self.state.message = message
                } else if !result.isLoading {
                    self.state.postId = Int(result.postId)
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

    deinit {
        crudBridge.dispose()
    }

    private static let groupId: Int32 = 0

    struct State {
        var text = ""
        var textError: String? = nil
        var isLoading = false
        var postId = -1
        var message = ""
    }
}
