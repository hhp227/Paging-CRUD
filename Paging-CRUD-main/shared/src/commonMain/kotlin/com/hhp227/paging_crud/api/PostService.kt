package com.hhp227.paging_crud.api

import com.hhp227.paging_crud.model.BasicApiResponse
import com.hhp227.paging_crud.model.ListItem
import com.hhp227.paging_crud.util.InjectorUtils
import com.hhp227.paging_crud.util.URLs
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.parameters

class PostService(private val client: HttpClient) {
    suspend fun getPostList(groupId: Int, offset: Int, loadSize: Int): BasicApiResponse<List<ListItem.Post>> {
        return client.get("${URLs.BASE_URL}/posts") {
            parameter("group_id", groupId)
            parameter("offset", offset)
            parameter("load_size", loadSize)
        }.body()
    }

    suspend fun addPost(apiKey: String, text: String, groupId: Int): BasicApiResponse<Int> {
        return client.submitForm(
            url = "${URLs.BASE_URL}/post",
            formParameters = parameters {
                append("text", text)
                append("group_id", groupId.toString())
            }
        ) {
            header(HttpHeaders.Authorization, apiKey)
        }.body()
    }

    suspend fun removePost(apiKey: String, postId: Int): BasicApiResponse<Unit> {
        return client.submitForm(
            url = "${URLs.BASE_URL}/post/$postId",
            formParameters = parameters {
                append("_METHOD", "DELETE")
            }
        ) {
            header(HttpHeaders.Authorization, apiKey)
        }.body()
    }

    companion object {
        fun create(): PostService {
            return PostService(InjectorUtils.provideHttpClient())
        }
    }
}
