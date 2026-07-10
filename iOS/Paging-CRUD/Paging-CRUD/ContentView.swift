//
//  ContentView.swift
//  Paging-CRUD
//
//  Created by 홍희표 on 7/10/26.
//

import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = PostViewModel()

    @State private var selectedPost: ListItem.Post?

    @State private var isCreatePresented = false

    var body: some View {
        NavigationView {
            ZStack(alignment: .bottomTrailing) {
                List(viewModel.state.itemList) { post in
                    PostRow(post: post)
                        .contentShape(Rectangle())
                        .onTapGesture {
                            selectedPost = post
                        }
                }
                .listStyle(.plain)
                .alert(
                    "게시글 삭제",
                    isPresented: Binding(
                        get: { selectedPost != nil },
                        set: { if !$0 { selectedPost = nil } }
                    ),
                    presenting: selectedPost
                ) { post in
                    Button("삭제", role: .destructive) {
                        viewModel.onDeletePost(post)
                    }
                    Button("취소", role: .cancel) {}
                } message: { _ in
                    Text("이 게시글을 삭제하시겠습니까?")
                }
                if !viewModel.state.isLoading && viewModel.state.itemList.isEmpty {
                    Text("게시물이 없습니다.")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
                if viewModel.state.isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
                Button {
                    isCreatePresented = true
                } label: {
                    Image(systemName: "plus")
                        .font(.title2)
                        .foregroundStyle(.white)
                        .frame(width: 56, height: 56)
                        .background(Circle().fill(Color.accentColor))
                        .shadow(radius: 4)
                }
                .padding(24)
            }
            .navigationTitle("Paging CRUD")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        viewModel.refresh()
                    } label: {
                        Image(systemName: "arrow.clockwise")
                    }
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
            .sheet(isPresented: $isCreatePresented) {
                CreateView {
                    viewModel.refresh()
                }
            }
        }
        .navigationViewStyle(.stack)
    }
}

struct PostRow: View {
    let post: ListItem.Post

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(post.name ?? "익명")
                    .font(.headline)
                Text(post.timeStamp ?? "")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Text(post.text)
                .font(.body)
            HStack {
                Text("댓글 \(post.replyCount)")
                Text("좋아요 \(post.likeCount)")
            }
            .font(.caption)
            .foregroundStyle(.secondary)
            .padding(.top, 4)
        }
        .padding(.vertical, 4)
    }
}

#Preview {
    ContentView()
}
