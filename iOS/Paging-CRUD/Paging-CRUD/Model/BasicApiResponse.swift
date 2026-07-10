//
//  BasicApiResponse.swift
//  Paging-CRUD
//

import Foundation

struct BasicApiResponse<T: Decodable>: Decodable {
    let error: Bool
    let message: String?
    let data: T?

    enum CodingKeys: String, CodingKey {
        case error
        case message
        case data = "result"
    }
}
