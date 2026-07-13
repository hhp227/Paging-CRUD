//
//  KmpInterop.swift
//  iosApp
//
//  shared(Kotlin)의 UseCase/Flow를 Compose ViewModel과 동일한 호출 패턴으로 쓰기 위한 셔거.
//  목표 표면 (Kotlin ↔ Swift):
//    getPostListUseCase(GROUP_ID).cachedIn(viewModelScope)
//    removePostUseCase(URLs.API_KEY, post.id).onEach { ... }.launchIn(viewModelScope)
//

import Foundation
import Shared

// shared의 URLs companion 상수를 Android 코드와 동일한 표기(URLs.API_KEY)로 쓰기 위한 셰도잉.
// (Shared 모듈의 URLs 프로토콜을 앱 모듈 선언이 가린다)
enum URLs {
    static let BASE_URL = URLsCompanion.shared.BASE_URL

    static let API_KEY = URLsCompanion.shared.API_KEY
}

// Android의 Resource sealed class 대응 — 단독 iOS 앱의 Resource.swift와 동일
enum Resource<T> {
    case success(T)
    case error(String, T? = nil)
    case loading(T? = nil)
}

// Kotlin의 Flow<Resource<T>>.onEach { }.launchIn(viewModelScope) 패턴 대응.
// 콜드 플로우처럼 launchIn 호출 시점에 shared 쪽 수집이 시작된다
struct ResourceFlow<T> {
    private let collect: (ViewModelScope, @escaping (Resource<T>) -> Void) -> Void

    private var action: ((Resource<T>) -> Void)?

    init(_ collect: @escaping (ViewModelScope, @escaping (Resource<T>) -> Void) -> Void) {
        self.collect = collect
    }

    func onEach(_ action: @escaping (Resource<T>) -> Void) -> ResourceFlow<T> {
        var flow = self
        flow.action = action
        return flow
    }

    func launchIn(_ scope: ViewModelScope) {
        let action = self.action
        collect(scope) { action?($0) }
    }
}

// Kotlin의 getPostListUseCase(GROUP_ID) 반환값(Flow<PagingData>) 대응.
// cachedIn(viewModelScope)이 shared 쪽 cachedIn + 브리지 생성으로 이어진다
struct PostPagingFlow {
    fileprivate let useCase: GetPostListUseCase

    fileprivate let groupId: Int32

    func cachedIn(_ scope: ViewModelScope) -> SwiftUiPagingBridge<ListItem.Post> {
        useCase.cachedIn(groupId: groupId, scope: scope)
    }
}

// Kotlin operator fun invoke와 동일한 호출 형태를 만드는 callAsFunction 셔거
extension GetPostListUseCase {
    func callAsFunction(groupId: Int32) -> PostPagingFlow {
        PostPagingFlow(useCase: self, groupId: groupId)
    }
}

extension AddPostUseCase {
    func callAsFunction(apiKey: String, groupId: Int32, text: String) -> ResourceFlow<Int> {
        ResourceFlow { scope, onEach in
            collectIn(scope: scope, apiKey: apiKey, groupId: groupId, text: text) { result in
                onEach(result.toResource { Int($0.postId) })
            }
        }
    }
}

extension RemovePostUseCase {
    func callAsFunction(apiKey: String, postId: Int32) -> ResourceFlow<Bool> {
        ResourceFlow { scope, onEach in
            collectIn(scope: scope, apiKey: apiKey, postId: postId) { result in
                onEach(result.toResource { _ in true })
            }
        }
    }
}

private extension PostOpResult {
    func toResource<T>(_ value: (PostOpResult) -> T) -> Resource<T> {
        if isLoading {
            return .loading()
        }
        if let errorMessage = errorMessage {
            return .error(errorMessage)
        }
        return .success(value(self))
    }
}
