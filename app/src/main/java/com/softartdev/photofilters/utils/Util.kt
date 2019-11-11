package com.softartdev.photofilters.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object Util {
    const val TAG = "PhotoFilterApp"

    fun getOutputMediaFile(contentResolver: ContentResolver): File? {
        val subDir = "Photo Filters"
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS", Locale.ROOT)
        val timeStamp = simpleDateFormat.format(Date())
        val fileName = "IMG_$timeStamp.jpg"

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            val values: ContentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/$subDir"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val collection =
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val item: Uri = contentResolver.insert(collection, values)!!
            Log.d(TAG, "media store uri: $item")

            contentResolver.openFileDescriptor(item, "w", null).use { pfd ->
                // Write data into the pending image.
            }
            // Now that we're finished, release the "pending" status, and allow other apps
            // to view the image.
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(item, values, null, null)
        }
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val mediaStorageDir = File(dir, subDir)
        mediaStorageDir.apply {
            if (!exists()) {
                if (!mkdirs()) {
                    Log.d(TAG, "failed to create directory")
                    return null
                }
            }
        }
        return File("${mediaStorageDir.path}${File.separator}$fileName")
    }
}