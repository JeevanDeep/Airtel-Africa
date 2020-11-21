package com.example.airtelafrica

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.airtelafrica.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException

private const val CAPTURE_FACE_REQUEST_CODE = 1001
private const val CAPTURE_ID_REQUEST_CODE = 1002
private const val PERMISSION_REQUEST_CODE = 1003

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var faceBitmap: Bitmap? = null
    private var idBitmap: Bitmap? = null
    private var lastRequestCode: Int = 0
    private var lastFileName = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        binding.captureFace.setOnClickListener { takeImage(CAPTURE_FACE_REQUEST_CODE, "FACE") }

        binding.captureID.setOnClickListener { takeImage(CAPTURE_ID_REQUEST_CODE, "USERID") }
    }

    private fun takeImage(requestCode: Int, fileName: String) {
        lastRequestCode = requestCode
        lastFileName = fileName
        if (hasPermissions()) {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try {
                val photoFile: File? = createImageFile(fileName)
                photoFile?.let {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                    startActivityForResult(takePictureIntent, requestCode)
                }
            } catch (e: ActivityNotFoundException) {
                // display error state to the user
                Toast.makeText(this, "No Camera App found", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    arrayOf(
                            android.Manifest.permission.CAMERA,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE
                    ), PERMISSION_REQUEST_CODE
            )
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(fileName: String): File? {
        // Create an image file name
        val storageDir: File = filesDir
        val image: File = File.createTempFile(
                fileName,  // prefix
                ".jpg",  // suffix
                storageDir // directory
        )

        // Save a file: path for use with ACTION_VIEW intents
        return image
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (hasPermissions()) {
            takeImage(lastRequestCode, lastFileName)
        }
    }
}