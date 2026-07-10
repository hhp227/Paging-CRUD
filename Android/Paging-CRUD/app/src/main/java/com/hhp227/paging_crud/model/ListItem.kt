package com.hhp227.paging_crud.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed class ListItem {
    @Serializable
    data class Post(
        @SerialName("id") var id: Int = 0,
        @SerialName("user_id") var userId: Int = 0,
        @SerialName("name") var name: String? = null,
        @SerialName("text") var text: String = "",
        @SerialName("status") var status: Int = 0,
        @SerialName("profile_img") var profileImage: String? = null,
        @SerialName("created_at") var timeStamp: String? = null,
        @SerialName("reply_count") var replyCount: Int = 0,
        @SerialName("like_count") var likeCount: Int = 0,
        @SerialName("report_count") var reportCount: Int = 0,
        @SerialName("attachment") var attachment: Attachment = Attachment()
    ) : ListItem()

    @Serializable
    data class Image(
        @SerialName("id") var id: Int = 0,
        @SerialName("image") var image: String? = null,
        @SerialName("tag") var tag: String? = null
    ) : ListItem()

    @Serializable
    data class Attachment(
        @SerialName("images") var imageItemList: List<Image> = emptyList(),
        @SerialName("video") var video: String? = null
    ) : ListItem()
}
