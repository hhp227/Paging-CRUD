package com.hhp227.paging_crud.util

import com.hhp227.paging_crud.api.PostService
import com.hhp227.paging_crud.data.PostDao
import com.hhp227.paging_crud.data.PostRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object InjectorUtils {
    fun getPostRepository() = PostRepository.getInstance(PostService.create(), PostDao)

    fun provideHttpClient(): HttpClient {
        val json = Json {
            isLenient = true
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        return HttpClient {
            install(ContentNegotiation) {
                // 서버가 JSON을 text/html 등으로 내려줘도 파싱되도록 모든 content-type에 적용
                json(json, contentType = ContentType.Any)
            }
        }
    }
}
