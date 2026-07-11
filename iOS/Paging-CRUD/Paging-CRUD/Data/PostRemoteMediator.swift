//
//  PostRemoteMediator.swift
//  Paging-CRUD
//

import Foundation
import Paging

final class PostRemoteMediator: RemoteMediator<Int, ListItem.Post> {
    private let postService: PostService

    private let postDao: PostDao

    private let groupId: Int

    init(postService: PostService, postDao: PostDao, groupId: Int) {
        self.postService = postService
        self.postDao = postDao
        self.groupId = groupId
        super.init()
    }

    override func load(loadType: LoadType, state: PagingState<Int, ListItem.Post>) async -> MediatorResult {
        do {
            let offset: Int

            switch loadType {
            case .refresh:
                offset = 0
            case .prepend:
                return .success(endOfPaginationReached: true)
            case .append:
                // 하단 도달시 로딩 인디케이터가 잠시 보이도록 의도적으로 지연
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                offset = postDao.getCount(groupId)
            }
            let loadSize = loadType == .refresh ? state.config.initialLoadSize : state.config.pageSize
            let response = try await postService.getPostList(groupId: groupId, offset: offset, loadSize: loadSize)

            if !response.error {
                let data = response.data ?? []

                if loadType == .refresh {
                    postDao.replaceAll(groupId, data)
                } else {
                    postDao.insertAll(groupId, data)
                }
                return .success(endOfPaginationReached: data.isEmpty)
            } else {
                return .error(
                    NSError(
                        domain: "PostRemoteMediator",
                        code: 0,
                        userInfo: [NSLocalizedDescriptionKey: response.message ?? "An unexpected error occured"]
                    )
                )
            }
        } catch {
            return .error(error)
        }
    }
}
