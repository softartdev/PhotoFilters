package com.softartdev.photofilters.ui

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
import kotlinx.android.synthetic.main.activity_filter.*

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

    override fun onChanged(bitmapListResult: BitmapListResult) = when (bitmapListResult.status) {
        ResultState.LOADING -> Unit//TODO
        ResultState.SUCCESS -> {
            val bitmapList: List<Bitmap> = bitmapListResult.bitmapList!!
            //Setup effect selector
            val blurBitmap = bitmapList[0]
            filter_blur_radio_button.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    filter_image_view.setImageBitmap(blurBitmap)
                }
            }
            filter_blur_radio_button.setThumbnail(blurBitmap)
            val convolveBitmap = bitmapList[1]
            filter_emboss_radio_button.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    filter_image_view.setImageBitmap(convolveBitmap)
                }
            }
            filter_emboss_radio_button.setThumbnail(convolveBitmap)
            val hueBitmap = bitmapList[2]
            filter_hue_radio_button.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    filter_image_view.setImageBitmap(hueBitmap)
                }
            }
            filter_hue_radio_button.setThumbnail(hueBitmap)

            filter_image_view.setImageBitmap(blurBitmap)
        }
        ResultState.ERROR -> Unit//TODO
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
