package com.softartdev.photofilters.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.renderscript.*
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.softartdev.photofilters.R
import kotlinx.android.synthetic.main.activity_filter.*
import kotlin.math.cos
import kotlin.math.sin

class FilterActivity : AppCompatActivity() {

    private var mBitmapIn: Bitmap? = null
    private var mBitmapsOut: Array<Bitmap>? = null
    private var mCurrentBitmap = 0

    private var mRS: RenderScript? = null
    private var mInAllocation: Allocation? = null
    private var mOutAllocations: Array<Allocation>? = null

    private var mScriptBlur: ScriptIntrinsicBlur? = null
    private var mScriptConvolve: ScriptIntrinsicConvolve5x5? = null
    private var mScriptMatrix: ScriptIntrinsicColorMatrix? = null

    private var mFilterMode = MODE_BLUR

    private var mLatestTask: RenderScriptTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter)

        val imageUri: Uri = intent.getParcelableExtra(EXTRA_IMAGE_URI)!!
//        filter_image_view.setImageURI(imageUri)

        // Set up main image view
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        mBitmapIn = BitmapFactory.decodeFile(imageUri.path, options)
        val width = mBitmapIn!!.width
        val height = mBitmapIn!!.height
        val config = mBitmapIn!!.config
        mBitmapsOut = Array(NUM_BITMAPS) { Bitmap.createBitmap(width, height, config) }

        filter_image_view.setImageBitmap(mBitmapsOut!![mCurrentBitmap])
        mCurrentBitmap += (mCurrentBitmap + 1) % NUM_BITMAPS

        //Set up seekbar
        filter_seek_bar.progress = 50
        filter_seek_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateImage(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        //Setup effect selector
        filter_blur_radio_button.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mFilterMode = MODE_BLUR
                updateImage(filter_seek_bar.progress)
            }
        }
        filter_emboss_radio_button.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mFilterMode = MODE_CONVOLVE
                updateImage(filter_seek_bar.progress)
            }
        }
        filter_hue_radio_button.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mFilterMode = MODE_COLORMATRIX
                updateImage(filter_seek_bar.progress)
            }
        }
        // Create renderScript
        createScript()
        // Create thumbnails
        createThumbnail()
        // Invoke renderScript kernel and update imageView
        mFilterMode = MODE_BLUR
        updateImage(50)
    }

    private fun createScript() {
        mRS = RenderScript.create(this)

        mInAllocation = Allocation.createFromBitmap(mRS, mBitmapIn)

        mOutAllocations = Array(NUM_BITMAPS) { i: Int ->
            Allocation.createFromBitmap(mRS, mBitmapsOut?.get(i))
        }
        // Create intrinsics.
        // RenderScript has built-in features such as blur, convolve filter etc.
        // These intrinsics are handy for specific operations without writing RenderScript kernel.
        // In the sample, it's creating blur, convolve and matrix intrinsics.
        mScriptBlur = ScriptIntrinsicBlur.create(mRS, Element.U8_4(mRS))
        mScriptConvolve = ScriptIntrinsicConvolve5x5.create(mRS, Element.U8_4(mRS))
        mScriptMatrix = ScriptIntrinsicColorMatrix.create(mRS)
    }

    private fun performFilter(inAllocation: Allocation, outAllocation: Allocation, bitmapOut: Bitmap, value: Float) {
        when (mFilterMode) {
            MODE_BLUR -> {
                // Set blur kernel size
                mScriptBlur?.setRadius(value)
                // Invoke filter kernel
                mScriptBlur?.setInput(inAllocation)
                mScriptBlur?.forEach(outAllocation)
            }
            MODE_CONVOLVE -> {
                val f2 = 1.0f - value
                // Emboss filter kernel
                val coefficients = floatArrayOf(
                    -value * 2, 0f, -value, 0f, 0f, 0f, -f2 * 2, -f2, 0f, 0f, -value, -f2, 1f, f2,
                    value, 0f, 0f, f2, f2 * 2, 0f, 0f, 0f,
                    value, 0f, value * 2
                )
                // Set kernel parameter
                mScriptConvolve?.setCoefficients(coefficients)
                // Invoke filter kernel
                mScriptConvolve?.setInput(inAllocation)
                mScriptConvolve?.forEach(outAllocation)
            }
            MODE_COLORMATRIX -> {
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
                mScriptMatrix?.setColorMatrix(mat)

                // Invoke filter kernel
                mScriptMatrix?.forEach(inAllocation, outAllocation)
            }
        }
        // Copy to bitmap and invalidate image view
        outAllocation.copyTo(bitmapOut)
    }

    /**
     * Convert seekBar progress parameter (0-100 in range) to parameter for each intrinsic filter.
     * (e.g. 1.0-25.0 in Blur filter)
     */
    private fun getFilterParameter(i: Int): Float {
        var f = 0f
        when (mFilterMode) {
            MODE_BLUR -> {
                val max = 25.0f
                val min = 1f
                f = ((max - min) * (i / 100.0) + min).toFloat()
            }
            MODE_CONVOLVE -> {
                val max = 2f
                val min = 0f
                f = ((max - min) * (i / 100.0) + min).toFloat()
            }
            MODE_COLORMATRIX -> {
                val max = Math.PI.toFloat()
                val min = (-Math.PI).toFloat()
                f = ((max - min) * (i / 100.0) + min).toFloat()
            }
        }
        return f
    }

    /**
     * In the AsyncTask, it invokes RenderScript intrinsics to do a filtering.
     *
     *
     * After the filtering is done, an operation blocks at Allocation.copyTo() in AsyncTask
     * thread. Once all operation is finished at onPostExecute() in UI thread, it can invalidate
     * and
     * update ImageView UI.
     */
    @SuppressLint("StaticFieldLeak")
    private inner class RenderScriptTask : AsyncTask<Float, Int, Int>() {

        private var mIssued: Boolean = false

        override fun doInBackground(vararg params: Float?): Int? {
            var index = -1
            if (!isCancelled) {
                mIssued = true
                index = mCurrentBitmap

                performFilter(mInAllocation!!, mOutAllocations!![index],
                    mBitmapsOut!![index], params[0]!!)
                mCurrentBitmap = (mCurrentBitmap + 1) % NUM_BITMAPS
            }
            return index
        }

        internal fun updateView(result: Int) {
            if (result != -1) {
                // Request UI update
                filter_image_view.setImageBitmap(mBitmapsOut?.get(result))
                filter_image_view.invalidate()
            }
        }

        override fun onPostExecute(result: Int) {
            updateView(result)
        }

        override fun onCancelled(result: Int) {
            if (mIssued) {
                updateView(result)
            }
        }
    }

    /**
     * Invoke AsyncTask and cancel previous task.
     *
     *
     * When AsyncTasks are piled up (typically in slow device with heavy kernel),
     * Only the latest (and already started) task invokes RenderScript operation.
     */
    private fun updateImage(progress: Int) {
        val f = getFilterParameter(progress)

        mLatestTask?.cancel(false)
        mLatestTask = RenderScriptTask()

        mLatestTask?.execute(f)
    }

    /**
     * Create thumbNail for UI. It invokes RenderScript kernel synchronously in UI-thread,
     * which is OK for small thumbnail (but not ideal).
     */
    private fun createThumbnail() {
        val width = 72
        val height = 96
        val scale = resources.displayMetrics.density
        val pixelsWidth = (width * scale + 0.5f).toInt()
        val pixelsHeight = (height * scale + 0.5f).toInt()

        // Temporary image
        val tempBitmap = Bitmap.createScaledBitmap(mBitmapIn!!, pixelsWidth, pixelsHeight, false)
        val inAllocation = Allocation.createFromBitmap(mRS, tempBitmap)

        // Create thumbnail with each RS intrinsic and set it to radio buttons
        val modes = intArrayOf(MODE_BLUR, MODE_CONVOLVE, MODE_COLORMATRIX)
        val ids = intArrayOf(R.id.filter_blur_radio_button, R.id.filter_emboss_radio_button, R.id.filter_hue_radio_button)
        val parameter = intArrayOf(50, 100, 25)
        for (mode in modes) {
            mFilterMode = mode
            val f = getFilterParameter(parameter[mode])

            val destBitmap = Bitmap.createBitmap(
                tempBitmap.width,
                tempBitmap.height, tempBitmap.config
            )
            val outAllocation = Allocation.createFromBitmap(mRS, destBitmap)
            performFilter(inAllocation, outAllocation, destBitmap, f)

            val button = findViewById<ThumbnailRadioButton>(ids[mode])
            button.setThumbnail(destBitmap)
        }
    }

    companion object {
        /**
         * Number of bitmaps that is used for renderScript thread and UI thread synchronization.
         */
        private const val NUM_BITMAPS = 2

        private const val MODE_BLUR = 0
        private const val MODE_CONVOLVE = 1
        private const val MODE_COLORMATRIX = 2

        private const val EXTRA_IMAGE_URI = "extra_image_uri"

        fun getStartIntent(context: Context, imageUri: Uri): Intent {
            val intent = Intent(context, FilterActivity::class.java)
            intent.putExtra(EXTRA_IMAGE_URI, imageUri)
            return intent
        }
    }
}
