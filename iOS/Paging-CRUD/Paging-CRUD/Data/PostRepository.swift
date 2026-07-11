//
//  PostRepository.swift
//  Paging-CRUD
//

import Foundation
import Combine
import Paging

final class PostRepository {
    private let postService: PostService

    private let localDataSource: PostDao

    private init(postService: PostService, localDataSource: PostDao) {
        self.postService = postService
        self.localDataSource = localDataSource
    }

    func getPostList(groupId: Int) -> AnyPublisher<PagingData<ListItem.Post>, Never> {
        // prefetchDistance = 1: 스크롤이 실제 바닥에 닿을 때만 다음 페이지 로드
        // initialLoadSize = 3페이지: refresh 윈도우가 [뷰포트 위 페이지, 앵커 페이지, 아래 페이지]를 덮어
        // 무효화 후에도 보던 내용이 유지되고, 바닥에 머무를 때 APPEND가 연쇄되지 않는다
        return Pager(
            PagingConfig(pageSize: Self.loadSize, prefetchDistance: 1, enablePlaceholders: false, initialLoadSize: Self.loadSize * 3),
            nil,
            PostRemoteMediator(postService: postService, postDao: localDataSource, groupId: groupId)
        ) {
            PostLocalPagingSource(postDao: self.localDataSource, groupId: groupId)
        }.publisher
    }

    func addPost(apiKey: String, groupId: Int, text: String) -> AsyncStream<Resource<Int>> {
        AsyncStream { continuation in
            Task {
                continuation.yield(.loading())
                do {
                    let response = try await postService.addPost(apiKey: apiKey, text: text, groupId: groupId)

                    if !response.error, let data = response.data {
                        continuation.yield(.success(data))
                    } else {
                        continuation.yield(.error(response.message ?? "An unexpected error occured"))
                    }
                } catch {
                    continuation.yield(.error(error.localizedDescription))
                }
                continuation.finish()
            }
        }
    }

    func removePost(apiKey: String, postId: Int) -> AsyncStream<Resource<Bool>> {
        AsyncStream { continuation in
            Task {
                continuation.yield(.loading())
                do {
                    let response = try await postService.removePost(apiKey: apiKey, postId: postId)

                    if !response.error {
                        localDataSource.deletePost(postId)
                        continuation.yield(.success(true))
                    } else {
                        continuation.yield(.error(response.message ?? "An unexpected error occured", false))
                    }
                } catch {
                    continuation.yield(.error(error.localizedDescription))
                }
                continuation.finish()
            }
        }
    }

    static let loadSize = 10

    static let shared = PostRepository(postService: PostService.create(), localDataSource: PostDao.shared)
}
