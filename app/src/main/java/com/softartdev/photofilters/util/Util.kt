package com.softartdev.photofilters.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.softartdev.photofilters.BuildConfig
import com.softartdev.photofilters.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object Util {
    const val TAG = "PhotoFilterApp"

    /** Helper function used to create a timestamped file */
    fun createFile(context: Context): File {
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, context.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        val baseFolder: File = if (mediaDir?.exists() == true) mediaDir else context.filesDir
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS", Locale.ROOT)
        val timeStamp = simpleDateFormat.format(System.currentTimeMillis())
        val name = "$timeStamp.jpg"
        return File(baseFolder, name)
    }

    fun uriFromFileProvider(context: Context, file: File): Uri {
        val authority = "${BuildConfig.APPLICATION_ID}.provider"
        return FileProvider.getUriForFile(context, authority, file)
    }
}