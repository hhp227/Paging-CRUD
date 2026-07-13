//
//  KmpInterop.swift
//  iosApp
//
//  Kotlin Flow ↔ Combine Publisher 대응.
//  UseCase 호출은 Combine Publisher를 반환하고, ViewModel은 표준 Combine
//  (.sink / .store(in: &cancellables))으로 소비한다 — Kotlin의
//  .onEach { }.launchIn(viewModelScope)에 해당하는 Combine 관용구.
//

import Combine
import Foundation
import Shared
// Paging 모듈 전체를 import하면 PagingData/LoadState 등이 Shared와 겹쳐 모호해지므로,
// 이 파일에 필요한 LazyPagingItems만 스코프 임포트한다
import class Paging.LazyPagingItems

// shared의 URLs companion 상수를 Android와 동일한 표기(URLs.API_KEY)로 쓰기 위한 셰도잉
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

// Kotlin: getPostListUseCase(GROUP_ID) → Flow<PagingData<Post>>
extension GetPostListUseCase {
    func callAsFunction(groupId: Int32) -> PostPagingPublisher {
        PostPagingPublisher(adapter: pagingFlow(groupId: groupId))
    }
}

// Kotlin: addPostUseCase(URLs.API_KEY, GROUP_ID, text) → Flow<Resource<Int>>
extension AddPostUseCase {
    func callAsFunction(apiKey: String, groupId: Int32, text: String) -> AnyPublisher<Resource<Int>, Never> {
        KotlinFlowPublisher<PostOpResult> { onEach in
            self.opFlow(apiKey: apiKey, groupId: groupId, text: text).subscribe(onEach: onEach)
        }
        .map { $0.toResource { Int($0.postId) } }
        .eraseToAnyPublisher()
    }
}

// Kotlin: removePostUseCase(URLs.API_KEY, postId) → Flow<Resource<Boolean>>
extension RemovePostUseCase {
    func callAsFunction(apiKey: String, postId: Int32) -> AnyPublisher<Resource<Bool>, Never> {
        KotlinFlowPublisher<PostOpResult> { onEach in
            self.opFlow(apiKey: apiKey, postId: postId).subscribe(onEach: onEach)
        }
        .map { $0.toResource { _ in true } }
        .eraseToAnyPublisher()
    }
}

// Kotlin의 Flow<PagingData<Post>> 대응 퍼블리셔.
// cachedIn()은 cachedIn(viewModelScope) 대응 — 캐시가 구독(cancellables) 수명에 묶인다
struct PostPagingPublisher: Publisher {
    typealias Output = PagingData<ListItem.Post>

    typealias Failure = Never

    fileprivate let adapter: PostPagingFlowAdapter

    func cachedIn() -> PostPagingPublisher {
        PostPagingPublisher(adapter: adapter.cachedIn())
    }

    func receive<S>(subscriber: S) where S: Subscriber, S.Input == Output, S.Failure == Never {
        KotlinFlowPublisher<Output> { onEach in
            self.adapter.subscribe(onEach: onEach)
        }
        .receive(subscriber: subscriber)
    }
}

// Compose의 pagingDataFlow.collectAsLazyPagingItems()와 동일한 소비 지점.
// State에서 꺼낸 PagingData 퍼블리셔를 presenter 브리지(PagingDataSubject)로 밀어넣는다.
// Output 제약 없이 받고 원소를 런타임 캐스팅한다(ObjC 제네릭 인자는 소거되므로 항상 성공).
// Paging 라이브러리의 동명 확장(Output == Paging.PagingData<T> 요구)과는 제약 불일치로 구분된다.
extension Publisher where Failure == Never {
    func collectAsLazyPagingItems() -> LazyPagingItems<ListItem.Post> {
        let subject = PagingDataSubject<ListItem.Post>()
        let bridge = unsafeDowncast(subject.bridge, to: SwiftUiPagingBridge<ListItem.Post>.self)
        let adapter = KmpPagingBridgeAdapter(bridge)

        adapter.retained = sink { subject.send(pagingData: $0 as! PagingData<ListItem.Post>) }
        return LazyPagingItems(bridge: adapter)
    }
}

// Kotlin FlowAdapter(콜드 Flow)를 Combine Publisher로 감싸는 어댑터
struct KotlinFlowPublisher<Output>: Publisher {
    typealias Failure = Never

    private let subscribe: (@escaping (Output) -> Void) -> FlowSubscription

    init(_ subscribe: @escaping (@escaping (Output) -> Void) -> FlowSubscription) {
        self.subscribe = subscribe
    }

    func receive<S>(subscriber: S) where S: Subscriber, S.Input == Output, S.Failure == Never {
        subscriber.receive(subscription: KotlinFlowSubscription(subscribe: subscribe, subscriber: subscriber))
    }
}

private final class KotlinFlowSubscription<S: Subscriber>: Subscription where S.Failure == Never {
    private let subscribe: (@escaping (S.Input) -> Void) -> FlowSubscription

    private var subscriber: S?

    private var kotlinSubscription: FlowSubscription?

    init(subscribe: @escaping (@escaping (S.Input) -> Void) -> FlowSubscription, subscriber: S) {
        self.subscribe = subscribe
        self.subscriber = subscriber
    }

    func request(_ demand: Subscribers.Demand) {
        guard kotlinSubscription == nil, let subscriber = subscriber else { return }

        kotlinSubscription = subscribe { value in
            _ = subscriber.receive(value)
        }
    }

    func cancel() {
        kotlinSubscription?.cancel()
        kotlinSubscription = nil
        subscriber = nil
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
