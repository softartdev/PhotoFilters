package com.softartdev.photofilters.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.renderscript.RenderScript
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.softartdev.photofilters.model.BitmapListResult

class FilterViewModel(
    imageUri: Uri,
    context: Context
) : ViewModel() {

    val filterLiveData: MutableLiveData<BitmapListResult> = MutableLiveData()

    private val filterTask: FilterTask

    init {
        filterLiveData.postValue(BitmapListResult.Loading)
        val drawable: Drawable = Drawable.createFromPath(imageUri.path)!!
        val bitmap: Bitmap = drawable.toBitmap()
        val rs = RenderScript.create(context)
        filterTask = FilterTask(bitmap, rs, filterLiveData)
        try {
            filterTask.execute()
        } catch (throwable: Throwable) {
            val error = BitmapListResult.Error(throwable.message)
            filterLiveData.postValue(error)
        }
    }

    override fun onCleared() {
        filterTask.cancel(true)
    }
}