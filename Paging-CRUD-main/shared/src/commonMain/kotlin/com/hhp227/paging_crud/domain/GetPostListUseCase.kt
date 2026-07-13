package com.hhp227.paging_crud.domain

import androidx.paging.PagingData
import com.hhp227.paging_crud.data.PostRepository
import com.hhp227.paging_crud.model.ListItem
import kotlinx.coroutines.flow.Flow

/**
 * 플랫폼 중립인 Flow<PagingData>를 cachedIn 없이 반환한다.
 * 캐시는 각 플랫폼의 프레젠테이션 경계에서 적용한다 — Android는 ViewModel의
 * viewModelScope에서, iOS는 iosMain의 asBridge()에서 (브리지가 scope를 소유).
 */
class GetPostListUseCase(private val repository: PostRepository) {
    operator fun invoke(groupId: Int): Flow<PagingData<ListItem.Post>> = repository.getPostList(groupId)
}
