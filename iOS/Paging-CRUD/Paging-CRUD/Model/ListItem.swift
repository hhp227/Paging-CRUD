//
//  ListItem.swift
//  Paging-CRUD
//

import Foundation

enum ListItem {
    struct Post: Codable, Identifiable, Hashable {
        var id: Int
        var userId: Int
        var name: String?
        var text: String
        var status: Int
        var profileImage: String?
        var timeStamp: String?
        var replyCount: Int
        var likeCount: Int
        var reportCount: Int
        var attachment: Attachment

        enum CodingKeys: String, CodingKey {
            case id
            case userId = "user_id"
            case name
            case text
            case status
            case profileImage = "profile_img"
            case timeStamp = "created_at"
            case replyCount = "reply_count"
            case likeCount = "like_count"
            case reportCount = "report_count"
            case attachment
        }
    }

    struct Image: Codable, Hashable {
        var id: Int?
        var image: String?
        var tag: String?
    }

    struct Attachment: Codable, Hashable {
        var imageItemList: [Image]
        var video: String?

        enum CodingKeys: String, CodingKey {
            case imageItemList = "images"
            case video
        }
    }
}
