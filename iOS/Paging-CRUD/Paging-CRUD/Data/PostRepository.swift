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
        return Pager(PagingConfig(pageSize: Self.loadSize, enablePlaceholders: false, initialLoadSize: Self.loadSize)) {
            PostPagingSource(postService: self.postService, postDao: self.localDataSource, groupId: groupId)
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

    func clearCache(groupId: Int) {
        localDataSource.deleteAll(groupId)
    }

    static let loadSize = 10

    static let shared = PostRepository(postService: PostService.create(), localDataSource: PostDao.shared)
}
