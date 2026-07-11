//
//  PostLocalPagingSource.swift
//  Paging-CRUD
//

import Foundation
import Paging

// 표시 목록은 항상 캐시의 0번부터 시작하는 프리픽스라서 무효화가 뷰포트 위쪽 행을 건드리지 않고
// (스크롤 유지), prepend는 발생하지 않는다. 아래로는 APPEND가 페이지 단위로 이어 붙인다
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
        // 하단 도달시 로딩 인디케이터가 1초 보인 뒤 다음 페이지가 나타나도록 의도적으로 지연
        if params is LoadParams<Int>.Append<Int> {
            try? await Task.sleep(nanoseconds: 1_000_000_000)
        }
        let count = postDao.getCount(groupId)
        let offset: Int
        let end: Int

        switch params {
        case is LoadParams<Int>.Prepend<Int>:
            return LoadResult<Int, ListItem.Post>.Page(data: [], prevKey: nil, nextKey: nil)
        case is LoadParams<Int>.Append<Int>:
            offset = params.getKey() ?? 0
            end = min(count, offset + params.loadSize)
        default:
            offset = 0
            end = min(count, params.getKey() ?? params.loadSize)
        }
        let data = postDao.getPostList(groupId, offset, end)
        return LoadResult<Int, ListItem.Post>.Page(
            data: data,
            prevKey: nil,
            nextKey: data.isEmpty || end >= count ? nil : end
        )
    }

    override func getRefreshKey(state: PagingState<Int, ListItem.Post>) -> Int? {
        // 무효화 전에 표시 중이던 프리픽스 전체를 그대로 다시 표시한다 (key = 표시 끝 오프셋)
        let presentedCount = state.pages.reduce(0) { $0 + $1.data.count }

        return presentedCount > 0 ? presentedCount : nil
    }
}
