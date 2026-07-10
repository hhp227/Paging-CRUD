package com.hhp227.paging_crud.util

import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.paging_crud.api.PostService
import com.hhp227.paging_crud.data.PostDao
import com.hhp227.paging_crud.data.PostRepository
import com.hhp227.paging_crud.viewmodel.CreatePostViewModel
import com.hhp227.paging_crud.viewmodel.PostViewModel
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object InjectorUtils {
    private fun getPostRepository() = PostRepository.getInstance(PostService.create(), PostDao)

    fun providePostViewModelFactory(): ViewModelProvider.Factory {
        return viewModelFactory {
            initializer {
                PostViewModel(getPostRepository())
            }
        }
    }

    fun provideCreatePostViewModelFactory(): ViewModelProvider.Factory {
        return viewModelFactory {
            initializer {
                CreatePostViewModel(getPostRepository())
            }
        }
    }

    fun provideRetrofit(): Retrofit {
        val json = Json {
            isLenient = true
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        val logger = HttpLoggingInterceptor { Log.d("API", it) }
        logger.level = HttpLoggingInterceptor.Level.BASIC
        val client = OkHttpClient.Builder()
            .addInterceptor(logger)
            .build()
        return Retrofit.Builder()
            .baseUrl("${URLs.BASE_URL}/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
