package com.hhp227.paging_crud.util

interface URLs {
    companion object {
        const val BASE_URL = "http://hong227.dothome.co.kr/hong227/v1"

        // 샘플앱에는 로그인이 없으므로 StoryGroup 계정의 api_key를 설정해야 게시글 작성/삭제가 가능하다
        const val API_KEY = "1b8211c0649c2fc380f61a66f04fc34f"
    }
}
