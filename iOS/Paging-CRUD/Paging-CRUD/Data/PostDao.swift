//
//  PostDao.swift
//  Paging-CRUD
//

import Foundation

// Post Caching
final class PostDao {
    static let shared = PostDao()

    private var cachedMap = [Int: [ListItem.Post]]()

    private var countMap = [Int: Int]()

    private let lock = NSLock()

    private init() {}

    func insertAll(_ key: Int, _ list: [ListItem.Post]) {
        lock.lock()
        defer { lock.unlock() }
        countMap[key] = 0
        cachedMap[key, default: []].append(contentsOf: list)
    }

    func getPostList(_ key: Int, _ start: Int, _ end: Int) -> [ListItem.Post] {
        lock.lock()
        defer { lock.unlock() }
        guard let list = cachedMap[key], start < list.count else { return [] }
        return Array(list[start..<min(end, list.count)])
    }

    func deletePost(_ postId: Int) {
        lock.lock()
        defer { lock.unlock() }
        guard let key = cachedMap.first(where: { $0.value.contains { $0.id == postId } })?.key else { return }

        if let count = countMap[key] {
            countMap[key] = count - 1
        }
        if let index = cachedMap[key]?.firstIndex(where: { $0.id == postId }) {
            cachedMap[key]?.remove(at: index)
        }
    }

    func deleteAll(_ key: Int) {
        lock.lock()
        defer { lock.unlock() }
        countMap[key] = 0
        cachedMap[key]?.removeAll()
    }

    func isCacheEmpty(_ key: Int) -> Bool {
        lock.lock()
        defer { lock.unlock() }
        return cachedMap[key, default: []].isEmpty
    }

    func getCount(_ key: Int) -> Int {
        lock.lock()
        defer { lock.unlock() }
        return countMap[key] ?? 0
    }
}
