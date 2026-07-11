//
//  PostLocalPagingSource.swift
//  Paging-CRUD
//

import Foundation
import Paging

final class PostLocalPagingSource: PagingSource<Int, ListItem.Post> {
    private let postDao: PostDao

    private let groupId: Int

    init(postDao: PostDao, groupId: Int) {
        self.postDao = postDao
        self.groupId = groupId
        super.init()

        let token = postDao.addInvalidationListener { [weak self] in
            self?.invalidate()
        }

        registerInvalidatedCallback { [postDao] in
            postDao.removeInvalidationListener(token)
        }
    }

    override func load(params: LoadParams<Int>) async -> LoadResult<Int, ListItem.Post> {
        let key = params.getKey() ?? 0
        let count = postDao.getCount(groupId)
        let offset: Int
        let limit: Int

        switch params {
        case is LoadParams<Int>.Prepend<Int>:
            offset = max(0, key - params.loadSize)
            limit = min(key, params.loadSize)
        case is LoadParams<Int>.Refresh<Int>:
            offset = key >= count ? max(0, count - params.loadSize) : key
            limit = params.loadSize
        default:
            offset = key
            limit = params.loadSize
        }
        let data = postDao.getPostList(groupId, offset, offset + limit)
        return LoadResult<Int, ListItem.Post>.Page(
            data: data,
            prevKey: offset <= 0 || data.isEmpty ? nil : offset,
            nextKey: data.isEmpty || offset + data.count >= count ? nil : offset + data.count
        )
    }

    override func getRefreshKey(state: PagingState<Int, ListItem.Post>) -> Int? {
        guard let anchorPosition = state.anchorPosition else {
            return nil
        }
        return max(0, anchorPosition - state.config.initialLoadSize / 2)
    }
}
