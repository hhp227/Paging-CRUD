//
//  PostDao.swift
//  Paging-CRUD
//

import Foundation

// Post Caching (Single Source of Truth)
final class PostDao {
    static let shared = PostDao()

    private var cachedMap = [Int: [ListItem.Post]]()

    private var invalidationListeners = [UUID: () -> Void]()

    private let lock = NSLock()

    private init() {}

    @discardableResult
    func addInvalidationListener(_ listener: @escaping () -> Void) -> UUID {
        lock.lock()
        defer { lock.unlock() }
        let token = UUID()

        invalidationListeners[token] = listener
        return token
    }

    func removeInvalidationListener(_ token: UUID) {
        lock.lock()
        defer { lock.unlock() }
        invalidationListeners[token] = nil
    }

    private func notifyInvalidated() {
        lock.lock()
        let listeners = Array(invalidationListeners.values)
        lock.unlock()
        listeners.forEach { $0() }
    }

    func insertAll(_ key: Int, _ list: [ListItem.Post]) {
        lock.lock()
        let cachedIds = Set(cachedMap[key, default: []].map { $0.id })

        cachedMap[key, default: []].append(contentsOf: list.filter { !cachedIds.contains($0.id) })
        lock.unlock()
        notifyInvalidated()
    }

    func replaceAll(_ key: Int, _ list: [ListItem.Post]) {
        lock.lock()
        cachedMap[key] = list
        lock.unlock()
        notifyInvalidated()
    }

    func getPostList(_ key: Int, _ start: Int, _ end: Int) -> [ListItem.Post] {
        lock.lock()
        defer { lock.unlock() }
        guard let list = cachedMap[key], start < list.count else { return [] }
        return Array(list[start..<min(end, list.count)])
    }

    func deletePost(_ postId: Int) {
        lock.lock()
        guard let key = cachedMap.first(where: { $0.value.contains { $0.id == postId } })?.key else {
            lock.unlock()
            return
        }

        cachedMap[key]?.removeAll { $0.id == postId }
        lock.unlock()
        notifyInvalidated()
    }

    func deleteAll(_ key: Int) {
        lock.lock()
        cachedMap[key]?.removeAll()
        lock.unlock()
        notifyInvalidated()
    }

    func isCacheEmpty(_ key: Int) -> Bool {
        lock.lock()
        defer { lock.unlock() }
        return cachedMap[key, default: []].isEmpty
    }

    func getCount(_ key: Int) -> Int {
        lock.lock()
        defer { lock.unlock() }
        return cachedMap[key]?.count ?? 0
    }
}
