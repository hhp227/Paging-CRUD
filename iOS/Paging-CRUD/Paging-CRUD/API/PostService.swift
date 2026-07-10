//
//  PostService.swift
//  Paging-CRUD
//

import Foundation

final class PostService {
    private let session: URLSession

    private let baseUrl = URL(string: "\(URLs.baseUrl)/")!

    init(session: URLSession = .shared) {
        self.session = session
    }

    func getPostList(groupId: Int, offset: Int, loadSize: Int) async throws -> BasicApiResponse<[ListItem.Post]> {
        var components = URLComponents(url: baseUrl.appendingPathComponent("posts"), resolvingAgainstBaseURL: false)!
        components.queryItems = [
            URLQueryItem(name: "group_id", value: String(groupId)),
            URLQueryItem(name: "offset", value: String(offset)),
            URLQueryItem(name: "load_size", value: String(loadSize))
        ]
        let (data, _) = try await session.data(from: components.url!)
        return try JSONDecoder().decode(BasicApiResponse<[ListItem.Post]>.self, from: data)
    }

    func addPost(apiKey: String, text: String, groupId: Int) async throws -> BasicApiResponse<Int> {
        let request = formUrlEncodedRequest(
            url: baseUrl.appendingPathComponent("post"),
            apiKey: apiKey,
            fields: ["text": text, "group_id": String(groupId)]
        )
        let (data, _) = try await session.data(for: request)
        return try JSONDecoder().decode(BasicApiResponse<Int>.self, from: data)
    }

    func removePost(apiKey: String, postId: Int) async throws -> BasicApiResponse<Bool> {
        let request = formUrlEncodedRequest(
            url: baseUrl.appendingPathComponent("post/\(postId)"),
            apiKey: apiKey,
            fields: ["_METHOD": "DELETE"]
        )
        let (data, _) = try await session.data(for: request)
        return try JSONDecoder().decode(BasicApiResponse<Bool>.self, from: data)
    }

    private func formUrlEncodedRequest(url: URL, apiKey: String, fields: [String: String]) -> URLRequest {
        var request = URLRequest(url: url)
        var allowedCharacters = CharacterSet.urlQueryAllowed
        allowedCharacters.remove(charactersIn: "&=+")
        request.httpMethod = "POST"
        request.setValue(apiKey, forHTTPHeaderField: "Authorization")
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        request.httpBody = fields
            .map { key, value in
                "\(key)=\(value.addingPercentEncoding(withAllowedCharacters: allowedCharacters) ?? value)"
            }
            .joined(separator: "&")
            .data(using: .utf8)
        return request
    }

    static func create() -> PostService {
        return PostService()
    }
}
