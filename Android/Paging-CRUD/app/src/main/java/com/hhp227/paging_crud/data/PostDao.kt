package com.hhp227.paging_crud.data

import com.hhp227.paging_crud.model.ListItem
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

// Post Caching (Single Source of Truth)
object PostDao {
    private val cachedMap = ConcurrentHashMap<Int, MutableList<ListItem.Post>>()

    private val invalidationListeners = CopyOnWriteArrayList<() -> Unit>()

    fun addInvalidationListener(listener: () -> Unit) {
        invalidationListeners += listener
    }

    fun removeInvalidationListener(listener: () -> Unit) {
        invalidationListeners -= listener
    }

    private fun notifyInvalidated() {
        invalidationListeners.forEach { it() }
    }

    fun insertAll(key: Int, list: List<ListItem.Post>) {
        cachedMap.computeIfAbsent(key) { mutableListOf() }.addAll(list)
        notifyInvalidated()
    }

    fun replaceAll(key: Int, list: List<ListItem.Post>) {
        cachedMap.compute(key) { _, _ -> list.toMutableList() }
        notifyInvalidated()
    }

    fun getPostList(key: Int, start: Int, end: Int): List<ListItem.Post> {
        return cachedMap[key]?.let { list ->
            if (start >= list.size) emptyList()
            else list.slice(start until if (end < list.size) end else list.size)
        } ?: emptyList()
    }

    fun deletePost(postId: Int) {
        val key = cachedMap.entries.find { entry -> entry.value.any { it.id == postId } }?.key ?: return

        if (cachedMap[key]?.removeAll { it.id == postId } == true) {
            notifyInvalidated()
        }
    }

    fun deleteAll(key: Int) {
        cachedMap[key]?.clear()
        notifyInvalidated()
    }

    fun isCacheEmpty(key: Int) = cachedMap.getOrDefault(key, emptyList()).isEmpty()

    fun getCount(key: Int) = cachedMap[key]?.size ?: 0
}
