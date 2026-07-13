//
//  ContentView.swift
//  iosApp
//

import SwiftUI
import Paging
import Shared

struct ContentView: View {
    @StateObject private var viewModel: PostViewModel

    @StateObject private var lazyPagingItems: LazyPagingItems<ListItem.Post>

    @State private var selectedPost: ListItem.Post?

    @State private var isCreatePresented = false

    init() {
        let viewModel = PostViewModel()

        _viewModel = StateObject(wrappedValue: viewModel)
        _lazyPagingItems = StateObject(wrappedValue: viewModel.makePagingItems())
    }

    var body: some View {
        NavigationView {
            ZStack(alignment: .bottomTrailing) {
                List {
                    ForEach(lazyPagingItems, key: { $0.id }) { post in
                        if let post = post {
                            PostRow(post: post)
                                .contentShape(Rectangle())
                                .onTapGesture {
                                    selectedPost = post
                                }
                        }
                    }
                    // mediator가 있으면 combined append는 mediator 상태를 따르므로, 캐시 페이지 로딩(source)도 함께 본다
                    if lazyPagingItems.loadState.append is LoadState.Loading || lazyPagingItems.loadState.source.append is LoadState.Loading {
                        HStack {
                            Spacer()
                            ProgressView()
                            Spacer()
                        }
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
                if lazyPagingItems.loadState.refresh is LoadState.NotLoading && lazyPagingItems.itemCount == 0 {
                    Text("게시물이 없습니다.")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
                if lazyPagingItems.loadState.refresh is LoadState.Error {
                    VStack(spacing: 8) {
                        Text("불러오기에 실패했습니다.")
                        Button("재시도") {
                            lazyPagingItems.retry()
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
                if lazyPagingItems.loadState.refresh is LoadState.Loading || viewModel.state.isLoading {
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
                        lazyPagingItems.refresh()
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
                    lazyPagingItems.refresh()
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
