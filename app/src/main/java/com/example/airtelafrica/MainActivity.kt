package com.example.airtelafrica

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.airtelafrica.Constants.CAPTURE_FACE_REQUEST_CODE
import com.example.airtelafrica.Constants.CAPTURE_ID_REQUEST_CODE
import com.example.airtelafrica.Constants.FACE_IMAGE_NAME
import com.example.airtelafrica.Constants.ID_IMAGE_NAME
import com.example.airtelafrica.Constants.PERMISSION_REQUEST_CODE
import com.example.airtelafrica.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var faceBitmap: Bitmap? = null
    private var idBitmap: Bitmap? = null
    private var lastRequestCode: Int = 0
    private var lastFileName = ""
    private var faceImageUri: Uri? = null
    private var idImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.captureFace.setOnClickListener { takeImage(ImageType.FACE) }

        binding.captureID.setOnClickListener { takeImage(ImageType.ID) }
    }

    private fun takeImage(imageType: ImageType) {
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(
                            android.Manifest.permission.CAMERA,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE
                    ), PERMISSION_REQUEST_CODE
            )
            return
        }
        try {
            val uri = if (imageType == ImageType.ID) {
                lastRequestCode = CAPTURE_ID_REQUEST_CODE
                lastFileName = ID_IMAGE_NAME
                idImageUri = createImageFile(ID_IMAGE_NAME)
                idImageUri
            } else {
                lastRequestCode = CAPTURE_FACE_REQUEST_CODE
                lastFileName = FACE_IMAGE_NAME
                faceImageUri = createImageFile(FACE_IMAGE_NAME)
                faceImageUri
            }
            uri?.let { openCamera(it, lastRequestCode) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openCamera(uri: Uri, requestCode: Int) {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            startActivityForResult(takePictureIntent, requestCode)
        } catch (e: ActivityNotFoundException) {
            // display error state to the user
            Toast.makeText(this, "No Camera App found", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(fileName: String): Uri? {
        lastFileName = fileName
        val photo = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.absolutePath + File.separator + fileName)
        val imageUri = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            // create Uri with 'file://' prefix
            Uri.fromFile(photo)
        } else {
            // create Uri with 'content://' prefix
            val authorities = applicationContext.packageName + ".provider"
            FileProvider.getUriForFile(this, authorities, photo)
        }
        return imageUri
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAPTURE_ID_REQUEST_CODE && resultCode == RESULT_OK) {
            Glide.with(this).load(idImageUri).into(binding.idImage)
        } else if (requestCode == CAPTURE_FACE_REQUEST_CODE && resultCode == RESULT_OK) {
            Glide.with(this).load(faceImageUri).into(binding.faceImage)
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (hasPermissions()) {
        }
    }
}