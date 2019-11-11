package com.softartdev.photofilters.model

import android.graphics.Bitmap

sealed class BitmapListResult(
    val status: ResultState,
    val bitmapList: List<Bitmap>? = null,
    val errorMessage: String? = null
) : Result<List<Bitmap>>(status, bitmapList, errorMessage) {

    data class Success(private val result: List<Bitmap>) : BitmapListResult(ResultState.SUCCESS, result)

    data class Error(private val error: String? = null) : BitmapListResult(ResultState.ERROR, errorMessage = error)

    object Loading : BitmapListResult(ResultState.LOADING)
}