package com.hhp227.paging_crud.data

import com.hhp227.paging_crud.model.ListItem
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

// Post Caching (Single Source of Truth)
// JVM 전용 ConcurrentHashMap 대신 atomicfu 락으로 KMP 공통 스레드 안전성을 확보한다
object PostDao {
    private val lock = SynchronizedObject()

    private val cachedMap = mutableMapOf<Int, MutableList<ListItem.Post>>()

    private val invalidationListeners = mutableListOf<() -> Unit>()

    fun addInvalidationListener(listener: () -> Unit) {
        synchronized(lock) { invalidationListeners += listener }
    }

    fun removeInvalidationListener(listener: () -> Unit) {
        synchronized(lock) { invalidationListeners -= listener }
    }

    private fun notifyInvalidated() {
        val listeners = synchronized(lock) { invalidationListeners.toList() }

        listeners.forEach { it() }
    }

    fun insertAll(key: Int, list: List<ListItem.Post>) {
        synchronized(lock) {
            val posts = cachedMap.getOrPut(key) { mutableListOf() }
            val cachedIds = posts.map { it.id }.toSet()

            posts += list.filter { it.id !in cachedIds }
        }
        notifyInvalidated()
    }

    fun replaceAll(key: Int, list: List<ListItem.Post>) {
        synchronized(lock) { cachedMap[key] = list.toMutableList() }
        notifyInvalidated()
    }

    fun getPostList(key: Int, start: Int, end: Int): List<ListItem.Post> {
        return synchronized(lock) {
            cachedMap[key]?.let { list ->
                if (start >= list.size) emptyList()
                else list.slice(start until if (end < list.size) end else list.size)
            } ?: emptyList()
        }
    }

    fun deletePost(postId: Int) {
        val removed = synchronized(lock) {
            val key = cachedMap.entries.find { entry -> entry.value.any { it.id == postId } }?.key
                ?: return

            cachedMap[key]?.removeAll { it.id == postId } == true
        }

        if (removed) {
            notifyInvalidated()
        }
    }

    fun deleteAll(key: Int) {
        synchronized(lock) { cachedMap[key]?.clear() }
        notifyInvalidated()
    }

    fun isCacheEmpty(key: Int) = synchronized(lock) { cachedMap[key]?.isEmpty() ?: true }

    fun getCount(key: Int) = synchronized(lock) { cachedMap[key]?.size ?: 0 }
}
