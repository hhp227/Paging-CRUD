//
//  Resource.swift
//  Paging-CRUD
//

import Foundation

enum Resource<T> {
    case success(T)
    case error(String, T? = nil)
    case loading(T? = nil)
}
