//
//  CreateView.swift
//  iosApp
//

import SwiftUI

struct CreateView: View {
    @StateObject private var viewModel = CreatePostViewModel()

    @Environment(\.dismiss) private var dismiss

    private let onPostCreated: () -> Void

    init(onPostCreated: @escaping () -> Void = {}) {
        self.onPostCreated = onPostCreated
    }

    var body: some View {
        NavigationView {
            ZStack {
                VStack(alignment: .leading) {
                    // Compose와 동일: value = state.text, onValueChange = viewModel::onTextChange
                    TextEditor(text: Binding(get: { viewModel.state.text }, set: viewModel.onTextChange))
                        .overlay(alignment: .topLeading) {
                            if viewModel.state.text.isEmpty {
                                Text("내용을 입력하세요.")
                                    .foregroundStyle(.secondary)
                                    .padding(.top, 8)
                                    .padding(.leading, 4)
                                    .allowsHitTesting(false)
                            }
                        }
                    if let textError = viewModel.state.textError {
                        Text(textError)
                            .font(.caption)
                            .foregroundStyle(.red)
                    }
                }
                .padding()
                if viewModel.state.isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            }
            .navigationTitle("글쓰기")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("취소") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("등록") {
                        viewModel.actionSend()
                    }
                    .disabled(viewModel.state.isLoading)
                }
            }
            .alert(
                "알림",
                isPresented: Binding(
                    get: { !viewModel.state.message.isEmpty },
                    set: { if !$0 { viewModel.onMessageShown() } }
                )
            ) {
                Button("확인", role: .cancel) {}
            } message: {
                Text(viewModel.state.message)
            }
            .onChange(of: viewModel.state.postId) { postId in
                if postId >= 0 {
                    onPostCreated()
                    dismiss()
                }
            }
        }
        .navigationViewStyle(.stack)
    }
}

#Preview {
    CreateView()
}
