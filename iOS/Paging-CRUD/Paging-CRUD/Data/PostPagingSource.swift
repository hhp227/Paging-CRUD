//
//  PostPagingSource.swift
//  Paging-CRUD
//

import Foundation
import Paging

final class PostPagingSource: PagingSource<Int, ListItem.Post> {
    private let postService: PostService

    private let postDao: PostDao

    private let groupId: Int

    init(postService: PostService, postDao: PostDao, groupId: Int) {
        self.postService = postService
        self.postDao = postDao
        self.groupId = groupId
        super.init()
    }

    override func load(params: LoadParams<Int>) async -> LoadResult<Int, ListItem.Post> {
        do {
            let offset = params.getKey() ?? 0
            let loadSize = PostRepository.loadSize
            let key = max(0, offset + postDao.getCount(groupId))
            let nextKey = key + loadSize
            let prevKey = key - loadSize
            let response = try await postService.getPostList(groupId: groupId, offset: key, loadSize: loadSize)

            if offset == 0 {
                postDao.deleteAll(groupId)
            }
            if !response.error {
                let data = response.data ?? []

                postDao.insertAll(groupId, data)
                return LoadResult<Int, ListItem.Post>.Page(
                    data: postDao.getPostList(groupId, key, nextKey),
                    prevKey: offset == 0 ? nil : prevKey,
                    nextKey: data.isEmpty ? nil : nextKey
                )
            } else {
                return LoadResult<Int, ListItem.Post>.Error(
                    error: NSError(
                        domain: "PostPagingSource",
                        code: 0,
                        userInfo: [NSLocalizedDescriptionKey: response.message ?? "An unexpected error occured"]
                    )
                )
            }
        } catch {
            return LoadResult<Int, ListItem.Post>.Error(error: error)
        }
    }

    override func getRefreshKey(state: PagingState<Int, ListItem.Post>) -> Int? {
        guard let anchorPosition = state.anchorPosition, !postDao.isCacheEmpty(groupId) else {
            return nil
        }
        return state.closestPageToPosition(anchorPosition)?.prevKey
    }
}
