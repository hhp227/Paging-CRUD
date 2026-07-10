package com.hhp227.paging_crud.api

import com.hhp227.paging_crud.model.BasicApiResponse
import com.hhp227.paging_crud.model.ListItem
import com.hhp227.paging_crud.util.InjectorUtils
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PostService {
    @GET("posts")
    suspend fun getPostList(
        @Query("group_id") groupId: Int,
        @Query("offset") offset: Int,
        @Query("load_size") loadSize: Int
    ): BasicApiResponse<List<ListItem.Post>>

    @POST("post")
    @FormUrlEncoded
    suspend fun addPost(
        @Header("Authorization") apiKey: String,
        @Field("text") text: String,
        @Field("group_id") groupId: Int
    ): BasicApiResponse<Int>

    @POST("post/{post_id}")
    @FormUrlEncoded
    suspend fun removePost(
        @Header("Authorization") apiKey: String,
        @Path("post_id") postId: Int,
        @Field("_METHOD") method: String = "DELETE"
    ): BasicApiResponse<Unit>

    companion object {
        fun create(): PostService {
            return InjectorUtils.provideRetrofit()
                .create(PostService::class.java)
        }
    }
}
