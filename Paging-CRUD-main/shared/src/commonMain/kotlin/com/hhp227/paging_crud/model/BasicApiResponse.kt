package com.hhp227.paging_crud.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BasicApiResponse<T>(
    @SerialName("error") val error: Boolean,
    @SerialName("message") val message: String? = null,
    // removePost 응답에는 result 키 자체가 없으므로 반드시 옵셔널로 둔다
    @SerialName("result") val data: T? = null
)
