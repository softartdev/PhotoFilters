package com.softartdev.photofilters.ui.filter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.renderscript.RenderScript
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.softartdev.photofilters.model.BitmapListResult

class FilterViewModel(
    private val imageUri: Uri,
    private val applicationContext: Context
) : ViewModel() {

    val filterLiveData: MutableLiveData<BitmapListResult> = MutableLiveData()

    private var filterTask: FilterTask? = null

    init {
        loadFilterResults()
    }

    fun loadFilterResults() = try {
        filterTask?.cancel(true)
        filterLiveData.postValue(BitmapListResult.Loading)
        val inputStream = applicationContext.contentResolver.openInputStream(imageUri)
        val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)
        val rs = RenderScript.create(applicationContext)
        filterTask = FilterTask(bitmap, rs, filterLiveData)
        filterTask?.execute()
    } catch (throwable: Throwable) {
        throwable.printStackTrace()
        val error = BitmapListResult.Error(throwable.message)
        filterLiveData.postValue(error)
    } finally {
        filterTask = null
    }

    override fun onCleared() {
        filterTask?.cancel(true)
    }
}