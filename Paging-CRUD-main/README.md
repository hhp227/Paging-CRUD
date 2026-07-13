# Paging-CRUD (Kotlin Multiplatform)

Android/iOS 단독 샘플과 동일한 페이징 로직을 KMP로 재구현한 프로젝트.
페이징 엔진은 양 플랫폼 모두 androidx **paging-common 3.3.6** (commonMain)이고,
UI 소비만 플랫폼별 라이브러리를 쓴다:

- **Android**: Paging3 `paging-compose`(`collectAsLazyPagingItems`)
- **iOS**: [Jetpack-Paging-for-SwiftUI](https://github.com/hhp227/Jetpack-Paging-for-SwiftUI)
  (`kmp-module` 브랜치, SPM) — `viewModel.pagingData.collectAsLazyPagingItems()`
  (내부적으로 `SwiftUiPagingBridge` → `KmpPagingBridgeAdapter`로 어댑팅)

## 구조

* [/shared](./shared/src) — 공통 로직 (양 플랫폼 동일 구조의 원본은 ../Android, ../iOS 샘플)
  - `commonMain` 데이터 계층: `PostDao`(인메모리 SSOT) + `PostLocalPagingSource`(프리픽스 표시) +
    `PostRemoteMediator`(REFRESH→replaceAll, APPEND→dao count 오프셋) + `PostRepository`,
    Ktor 기반 `PostService`
  - `commonMain` 도메인 계층: `GetPostListUseCase`(cachedIn 없는 Flow<PagingData> 반환) +
    `AddPostUseCase` + `RemovePostUseCase` — 캐시는 각 플랫폼 프레젠테이션 경계에서 적용
    (Android는 ViewModel `cachedIn(viewModelScope)`, iOS는 `asBridge()` 내부)
  - `iosMain`: `SwiftUiPagingBridge` 벤더링(라이브러리 kmp-module 브랜치 출처) +
    `ViewModelScope`/`cachedIn`/`collectIn` (Swift 진입점 — 앱의 KmpInterop.swift와 조합되어
    Compose ViewModel과 동일한 호출 패턴을 만든다)
* [/composeApp](./composeApp/src) — Android 앱 (androidMain 전용, Compose + Paging3)
* [/iosApp](./iosApp/iosApp) — iOS 앱 (SwiftUI, SPM으로 Paging 라이브러리 사용)

## 빌드

- Android: `./gradlew :composeApp:assembleDebug`
  (WSL에서는 Windows gradle.bat 사용 — 저장소 메모 참고)
- iOS: [/iosApp](./iosApp)을 Xcode로 열어 실행 (Mac에서 shared 프레임워크가 함께 빌드됨)
