package com.softartdev.photofilters

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraX
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureConfig
import androidx.camera.core.PreviewConfig
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.softartdev.photofilters.utils.AutoFitPreviewBuilder
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private var lensFacing = CameraX.LensFacing.BACK
    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        lensFacing =
            savedInstanceState?.getInt(KEY_LENS_FACING)?.let { CameraX.LensFacing.values()[it] }
                ?: CameraX.LensFacing.BACK
        // Request camera permissions
        if (allPermissionsGranted()) {
            view_finder.post {
                updateCameraUi()
                bindCameraUseCases()
            }
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun updateCameraUi() {
        camera_switch_button.setOnClickListener {
            lensFacing = when (lensFacing) {
                CameraX.LensFacing.FRONT -> CameraX.LensFacing.BACK
                CameraX.LensFacing.BACK -> CameraX.LensFacing.FRONT
            }
            try {
                // Unbind all use cases and bind them again with the new lens facing configuration
                CameraX.unbindAll()
                bindCameraUseCases()
            } catch (exc: Exception) {
                // Do nothing
            }
        }
        camera_capture_button.setOnClickListener {
            val imageSavedListener = createImageSavedListener()
            val metadata = ImageCapture.Metadata().apply {
                isReversedHorizontal = lensFacing == CameraX.LensFacing.FRONT
            }
            imageCapture?.takePicture(getOutputMediaFile(), imageSavedListener, metadata)
        }
        photo_view_button.setOnClickListener {
            val openImageIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("image/*")
            startActivityForResult(openImageIntent, READ_REQUEST_CODE)
        }
    }

    private fun bindCameraUseCases() {
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { view_finder.display.getRealMetrics(it) }
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)
        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)
            // We request aspect ratio but no resolution to let CameraX optimize our use cases
            setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            setTargetRotation(view_finder.display.rotation)
        }.build()

        // Build the viewfinder use case
        val preview = AutoFitPreviewBuilder.build(previewConfig, view_finder)

        // Create configuration object for the image capture use case
        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setLensFacing(lensFacing)
            setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            // We request aspect ratio but no resolution to match preview config but letting
            // CameraX optimize for whatever specific resolution best fits requested capture mode
            setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            setTargetRotation(view_finder.display.rotation)
        }.build()

        // Build the image capture use case and attach button click listener
        imageCapture = ImageCapture(imageCaptureConfig)

        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    private fun createImageSavedListener(): ImageCapture.OnImageSavedListener =
        object : ImageCapture.OnImageSavedListener {
            override fun onImageSaved(photoFile: File) {
                Log.d(TAG, "Photo capture succeeded: ${photoFile.absolutePath}")

                val photoUri = Uri.fromFile(photoFile)
                startActivity(FilterActivity.getStartIntent(this@MainActivity, photoUri))

                // Implicit broadcasts will be ignored for devices running API
                // level >= 24, so if you only target 24+ you can remove this statement
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    sendBroadcast(Intent(Camera.ACTION_NEW_PICTURE, photoUri))
                }
                // If the folder selected is an external media directory, this is unnecessary
                // but otherwise other apps will not be able to access our images unless we
                // scan them using [MediaScannerConnection]
                val mimeType =
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(photoFile.extension)
                MediaScannerConnection.scanFile(
                    this@MainActivity,
                    arrayOf(photoFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )
            }

            override fun onError(
                imageCaptureError: ImageCapture.ImageCaptureError,
                message: String,
                cause: Throwable?
            ) {
                val msg = "Photo capture failed: $message"
                Log.e(TAG, msg, cause)
                view_finder.post {
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun getOutputMediaFile(): File? {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val subDir = "Photo_Filters"
        val mediaStorageDir = File(dir, subDir)
        mediaStorageDir.apply {
            if (!exists()) {
                if (!mkdirs()) {
                    Log.d(TAG, "failed to create directory")
                    return null
                }
            }
        }
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS", Locale.ROOT)
        val timeStamp = simpleDateFormat.format(Date())
        return File("${mediaStorageDir.path}${File.separator}IMG_$timeStamp.jpg")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) =
        if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let {
                Log.i(TAG, "picked image: $it")
                startActivity(FilterActivity.getStartIntent(this@MainActivity, it))
            } ?: Log.e(TAG, "picked image is null"); Unit
        } else super.onActivityResult(requestCode, resultCode, data)

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                view_finder.post {
                    updateCameraUi()
                    bindCameraUseCases()
                }
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted(): Boolean = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_LENS_FACING, lensFacing.ordinal)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val TAG = "PhotoFilterApp"
        private const val KEY_LENS_FACING = "key_lens_facing"
        private const val READ_REQUEST_CODE = 42
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}
