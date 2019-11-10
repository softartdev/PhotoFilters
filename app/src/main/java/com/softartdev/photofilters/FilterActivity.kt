package com.softartdev.photofilters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_filter.*

class FilterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter)

        val imageUri: Uri = intent.getParcelableExtra(EXTRA_IMAGE_URI)!!
        filter_image_view.setImageURI(imageUri)
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
