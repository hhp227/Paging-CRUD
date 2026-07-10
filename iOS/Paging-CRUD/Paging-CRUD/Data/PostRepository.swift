//
//  PostRepository.swift
//  Paging-CRUD
//

import Foundation

final class PostRepository {
    private let postService: PostService

    private init(postService: PostService) {
        self.postService = postService
    }

    // TODO 스크롤이 하단에 도달시 offset으로 다음페이지를 불러오는 로직은 추후 구현
    func getPostList(groupId: Int, offset: Int) -> AsyncStream<Resource<[ListItem.Post]>> {
        AsyncStream { continuation in
            Task {
                continuation.yield(.loading())
                do {
                    let response = try await postService.getPostList(groupId: groupId, offset: offset, loadSize: Self.loadSize)

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

    static let shared = PostRepository(postService: PostService.create())
}
