package com.softartdev.photofilters.ui.filter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.renderscript.RenderScript
import androidx.exifinterface.media.ExifInterface
import androidx.exifinterface.media.ExifInterface.*
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
        val bitmap = rotatedBitmap()
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

    private fun rotatedBitmap(): Bitmap {
        val inputStream = applicationContext.contentResolver.openInputStream(imageUri)!!
        val src: Bitmap = BitmapFactory.decodeStream(inputStream)

        val exifInterface = ExifInterface(inputStream)
        val orientation = exifInterface.getAttributeInt(TAG_ORIENTATION, ORIENTATION_UNDEFINED)
        val degrees = when (orientation) {
            ORIENTATION_ROTATE_90 -> 90f
            ORIENTATION_ROTATE_180 -> 180f
            ORIENTATION_ROTATE_270 -> 270f
            else -> return src
        }
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    override fun onCleared() {
        filterTask?.cancel(true)
    }
}