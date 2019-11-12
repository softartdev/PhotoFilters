package com.softartdev.photofilters.ui.filter

import android.graphics.Bitmap
import android.os.AsyncTask
import android.renderscript.*
import androidx.lifecycle.MutableLiveData
import com.softartdev.photofilters.model.BitmapListResult
import kotlin.math.cos
import kotlin.math.sin

class FilterTask(
    private val bitmap: Bitmap,
    private val rs: RenderScript,
    private val filterLiveData: MutableLiveData<BitmapListResult>
) : AsyncTask<Void, Void, List<Bitmap>>() {

    override fun doInBackground(vararg params: Void?): List<Bitmap>? {
        val width = bitmap.width
        val height = bitmap.height
        val config = bitmap.config
        val bitmapsOut: Array<Bitmap> = Array(NUM_BITMAPS) {
            Bitmap.createBitmap(width, height, config)
        }
        val inAllocation: Allocation = Allocation.createFromBitmap(rs, bitmap)
        val outAllocations: Array<Allocation> = Array(NUM_BITMAPS) { i: Int ->
            Allocation.createFromBitmap(rs, bitmapsOut[i])
        }
        val modes = intArrayOf(
            MODE_ORIGINAL,
            MODE_BLUR,
            MODE_CONVOLVE,
            MODE_COLOR_MATRIX
        )
        val parameter = intArrayOf(0, 50, 100, 25)
        (0 until NUM_BITMAPS).forEach { i ->
            val outAllocation = outAllocations[i]
            val bitmapOut = bitmapsOut[i]
            val mode = modes[i]
            val seek = parameter[mode]
            val value = getFilterParameter(mode, seek)
            performFilter(mode, inAllocation, outAllocation, bitmapOut, value)
        }
        return bitmapsOut.toList()
    }

    override fun onPostExecute(result: List<Bitmap>) {
        val bitmapListResult = BitmapListResult.Success(result)
        filterLiveData.postValue(bitmapListResult)
    }

    /**
     * Convert seekBar progress parameter (0-100 in range) to parameter for each intrinsic filter.
     * (e.g. 1.0-25.0 in Blur filter)
     */
    private fun getFilterParameter(mode: Int, seek: Int): Float {
        var f = 0f
        when (mode) {
            MODE_ORIGINAL -> return f
            MODE_BLUR -> {
                val max = 25.0f
                val min = 1f
                f = ((max - min) * (seek / 100.0) + min).toFloat()
            }
            MODE_CONVOLVE -> {
                val max = 2f
                val min = 0f
                f = ((max - min) * (seek / 100.0) + min).toFloat()
            }
            MODE_COLOR_MATRIX -> {
                val max = Math.PI.toFloat()
                val min = (-Math.PI).toFloat()
                f = ((max - min) * (seek / 100.0) + min).toFloat()
            }
        }
        return f
    }

    private fun performFilter(mode: Int, inAllocation: Allocation, outAllocation: Allocation, bitmapOut: Bitmap, value: Float) {
        when (mode) {
            MODE_ORIGINAL -> {
                inAllocation.copyTo(bitmapOut)
                return
            }
            MODE_BLUR -> {
                val scriptBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
                // Set blur kernel size
                scriptBlur.setRadius(value)
                // Invoke filter kernel
                scriptBlur.setInput(inAllocation)
                scriptBlur.forEach(outAllocation)
            }
            MODE_CONVOLVE -> {
                val f2 = 1.0f - value
                // Emboss filter kernel
                val coefficients = floatArrayOf(
                    -value * 2, 0f, -value, 0f, 0f, 0f, -f2 * 2, -f2, 0f, 0f, -value, -f2, 1f, f2,
                    value, 0f, 0f, f2, f2 * 2, 0f, 0f, 0f,
                    value, 0f, value * 2
                )
                val scriptConvolve = ScriptIntrinsicConvolve5x5.create(rs, Element.U8_4(rs))
                // Set kernel parameter
                scriptConvolve.setCoefficients(coefficients)
                // Invoke filter kernel
                scriptConvolve.setInput(inAllocation)
                scriptConvolve.forEach(outAllocation)
            }
            MODE_COLOR_MATRIX -> {
                val scriptMatrix = ScriptIntrinsicColorMatrix.create(rs)
                // Set HUE rotation matrix
                // The matrix below performs a combined operation of,
                // RGB->HSV transform * HUE rotation * HSV->RGB transform
                val cos = cos(value.toDouble()).toFloat()
                val sin = sin(value.toDouble()).toFloat()
                val mat = Matrix3f()
                mat.set(0, 0, (.299 + .701 * cos + .168 * sin).toFloat())
                mat.set(1, 0, (.587 - .587 * cos + .330 * sin).toFloat())
                mat.set(2, 0, (.114 - .114 * cos - .497 * sin).toFloat())
                mat.set(0, 1, (.299 - .299 * cos - .328 * sin).toFloat())
                mat.set(1, 1, (.587 + .413 * cos + .035 * sin).toFloat())
                mat.set(2, 1, (.114 - .114 * cos + .292 * sin).toFloat())
                mat.set(0, 2, (.299 - .3 * cos + 1.25 * sin).toFloat())
                mat.set(1, 2, (.587 - .588 * cos - 1.05 * sin).toFloat())
                mat.set(2, 2, (.114 + .886 * cos - .203 * sin).toFloat())
                scriptMatrix.setColorMatrix(mat)

                // Invoke filter kernel
                scriptMatrix.forEach(inAllocation, outAllocation)
            }
        }
        // Copy to bitmap and invalidate image view
        outAllocation.copyTo(bitmapOut)
    }

    companion object {
        /**
         * Number of bitmaps that is used for renderScript thread and UI thread synchronization.
         */
        private const val NUM_BITMAPS = 4
        private const val MODE_ORIGINAL = 0
        private const val MODE_BLUR = 1
        private const val MODE_CONVOLVE = 2
        private const val MODE_COLOR_MATRIX = 3
    }
}