package com.softartdev.photofilters.model

open class Result<out T> constructor(
    val state: ResultState,
    val data: T?,
    val message: String?
) {

    fun <T> success(data: T): Result<T> {
        return Result(ResultState.SUCCESS, data, null)
    }

    fun <T> error(message: String, data: T?): Result<T> {
        return Result(ResultState.ERROR, null, message)
    }

    fun <T> loading(): Result<T> {
        return Result(ResultState.LOADING, null, null)
    }

}