package com.softartdev.photofilters.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraX
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureConfig
import androidx.camera.core.PreviewConfig
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.softartdev.photofilters.R
import com.softartdev.photofilters.ui.filter.FilterActivity
import com.softartdev.photofilters.util.AutoFitPreviewBuilder
import com.softartdev.photofilters.util.Util
import com.softartdev.photofilters.util.Util.TAG
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity() {

    private var lensFacing = CameraX.LensFacing.BACK
    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        savedInstanceState?.getInt(KEY_LENS_FACING)?.let {
            lensFacing = CameraX.LensFacing.values()[it]
        }
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
                CameraX.unbindAll()
                bindCameraUseCases()
            } catch (exc: Exception) {
                // Do nothing
            }
        }
        camera_capture_button.setOnClickListener {
            val file = Util.getOutputMediaFile(contentResolver)
            val imageSavedListener = createImageSavedListener()
            val metadata = ImageCapture.Metadata().apply {
                isReversedHorizontal = lensFacing == CameraX.LensFacing.FRONT
            }
            imageCapture?.takePicture(file, imageSavedListener, metadata)
        }
        photo_view_button.setOnClickListener {
            val openImageIntent = Intent(Intent.ACTION_GET_CONTENT)
                .setType("image/*")
            startActivityForResult(openImageIntent, IMAGE_REQUEST_CODE)
        }
    }

    private fun bindCameraUseCases() {
        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)
            setTargetRotation(view_finder.display.rotation)
        }.build()
        val preview = AutoFitPreviewBuilder.build(previewConfig, view_finder)

        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setLensFacing(lensFacing)
            setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            setTargetRotation(view_finder.display.rotation)
        }.build()
        imageCapture = ImageCapture(imageCaptureConfig)

        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    private fun createImageSavedListener(): ImageCapture.OnImageSavedListener =
        object : ImageCapture.OnImageSavedListener {
            override fun onImageSaved(photoFile: File) {
                Log.d(TAG, "Photo capture succeeded: ${photoFile.absolutePath}")
                val photoUri = Uri.fromFile(photoFile)
                startActivity(FilterActivity.getStartIntent(this@MainActivity, photoUri))
            }

            override fun onError(e: ImageCapture.ImageCaptureError, msg: String, t: Throwable?) {
                val message = "Photo capture failed: $msg"
                Log.e(TAG, message, t)
                view_finder.post {
                    Toast.makeText(baseContext, message, Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) =
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == RESULT_OK) {
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
        private const val KEY_LENS_FACING = "key_lens_facing"
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val IMAGE_REQUEST_CODE = 42
    }
}
