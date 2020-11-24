package com.example.airtelafrica

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.RectF
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
import com.bumptech.glide.signature.ObjectKey
import com.example.airtelafrica.Constants.CAPTURE_FACE_REQUEST_CODE
import com.example.airtelafrica.Constants.CAPTURE_ID_REQUEST_CODE
import com.example.airtelafrica.Constants.FACE_IMAGE_NAME
import com.example.airtelafrica.Constants.ID_IMAGE_NAME
import com.example.airtelafrica.Constants.PERMISSION_REQUEST_CODE
import com.example.airtelafrica.databinding.ActivityMainBinding
import com.example.airtelafrica.tflite.SimilarityClassifier
import com.example.airtelafrica.tflite.TFLiteObjectDetectionAPIModel
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import java.io.File
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var lastRequestCode: Int = 0
    private var lastFileName = ""
    private var faceImageUri: Uri? = null
    private var idImageUri: Uri? = null

    private val TF_OD_API_INPUT_SIZE = 223
    private val TF_OD_API_IS_QUANTIZED = false
    private val TF_OD_API_MODEL_FILE = "mobile_face_net.tflite"
    private val TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt"
    private val detector: FirebaseVisionFaceDetector by lazy {
        val highAccuracyOpts = FirebaseVisionFaceDetectorOptions.Builder()
                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .build()
        FirebaseVision.getInstance().getVisionFaceDetector(highAccuracyOpts)
    }

    private val facialRecognition by lazy {
        TFLiteObjectDetectionAPIModel.create(assets,
                TF_OD_API_MODEL_FILE,
                TF_OD_API_LABELS_FILE,
                TF_OD_API_INPUT_SIZE,
                TF_OD_API_IS_QUANTIZED).apply {
            setNumThreads(1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        setOnClickListener()
    }

    private fun setOnClickListener() {
        with(binding) {
            captureFace.setOnClickListener { takeImage(ImageType.FACE) }
            captureID.setOnClickListener { takeImage(ImageType.ID) }

            compareButton.setOnClickListener {
                if (idImageUri != null && faceImageUri != null) {
                    compareImages()
                }
            }
        }
    }

    private fun compareImages() {
        val faceImage = FirebaseVisionImage.fromFilePath(this, faceImageUri!!)
        val scaledBitmap = Bitmap.createScaledBitmap(faceImage.bitmap, 224, 224, false)

        detector.detectInImage(FirebaseVisionImage.fromBitmap(scaledBitmap))
                .addOnSuccessListener { faces ->
                    if (faces.size == 1) {
                        facialRecognition.register("face", SimilarityClassifier.Recognition(
                                "face",
                                "face",
                                .7f,
                                RectF(faces[0].boundingBox)
                        ))
                        checkForSimilarity()
                    } else {
                        // either zero faces or more than 1 face. show error
                    }
                }
                .addOnFailureListener { e ->

                }

    }

    private fun checkForSimilarity() {
        val idImage = FirebaseVisionImage.fromFilePath(this, idImageUri!!)
        val scaledBitmap = Bitmap.createScaledBitmap(idImage.bitmap, 224, 224, false)
        detector.detectInImage(FirebaseVisionImage.fromBitmap(scaledBitmap))
                .addOnSuccessListener { faces ->
                    if (faces.size == 1) {
                        //the below code is crashing. need to fix
//                        val ans = facialRecognition.recognizeImage(scaledBitmap, true)
                    } else {
                        // either zero faces or more than 1 face. show error
                    }
                }
                .addOnFailureListener { e ->

                }
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
            Glide.with(this).load(idImageUri).signature(ObjectKey(System.currentTimeMillis())).into(binding.idImage)
        } else if (requestCode == CAPTURE_FACE_REQUEST_CODE && resultCode == RESULT_OK) {
            Glide.with(this).load(faceImageUri).signature(ObjectKey(System.currentTimeMillis())).into(binding.faceImage)
        }
    }
}