package com.softartdev.photofilters.ui.filter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.softartdev.photofilters.R
import com.softartdev.photofilters.model.BitmapListResult
import com.softartdev.photofilters.model.ResultState
import com.softartdev.photofilters.util.gone
import com.softartdev.photofilters.util.visible
import kotlinx.android.synthetic.main.activity_filter.*
import kotlinx.android.synthetic.main.content_filter.*
import kotlinx.android.synthetic.main.view_error.view.*

class FilterActivity : AppCompatActivity(), Observer<BitmapListResult> {

    private lateinit var viewModel: FilterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter)

        val imageUri: Uri = intent.getParcelableExtra(EXTRA_IMAGE_URI)!!

        val viewModelFactory = FilterViewModelFactory(imageUri, applicationContext)
        viewModel = ViewModelProviders.of(this, viewModelFactory)[FilterViewModel::class.java]
        viewModel.filterLiveData.observe(this, this)
    }

    override fun onChanged(result: BitmapListResult) = when (result.status) {
        ResultState.LOADING -> {
            filter_progress_view.visible()
            filter_content_view.gone()
            filter_error_view.gone()
        }
        ResultState.SUCCESS -> {
            filter_progress_view.gone()
            filter_content_view.apply {
                val bitmapList: List<Bitmap> = result.bitmapList!!

                val blurBitmap = bitmapList[0]
                setThumbnail(filter_blur_radio_button, blurBitmap)

                val convolveBitmap = bitmapList[1]
                setThumbnail(filter_emboss_radio_button, convolveBitmap)

                val hueBitmap = bitmapList[2]
                setThumbnail(filter_hue_radio_button, hueBitmap)

                filter_image_view.setImageBitmap(blurBitmap)
            }.visible()
            filter_error_view.gone()
        }
        ResultState.ERROR -> {
            filter_progress_view.gone()
            filter_content_view.gone()
            filter_error_view.apply {
                val message = result.errorMessage ?: getString(R.string.label_error_result)
                error_message_text_view.text = message
                error_try_again_button.setOnClickListener { viewModel.loadFilterResults() }
            }.visible()
        }
    }

    private fun setThumbnail(button: ThumbnailRadioButton, bitmap: Bitmap) = button.apply {
        setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                filter_image_view.setImageBitmap(bitmap)
            }
        }
        setThumbnail(bitmap)
    }

    companion object {
        private const val EXTRA_IMAGE_URI = "extra_image_uri"

        fun getStartIntent(context: Context, imageUri: Uri): Intent {
            val intent = Intent(context, FilterActivity::class.java)
            intent.putExtra(EXTRA_IMAGE_URI, imageUri)
            return intent
        }
    }
}
