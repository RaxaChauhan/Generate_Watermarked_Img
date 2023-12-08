package com.example.whatermarkimg

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var img: ImageView
    private lateinit var selectedImageUri: Uri
    private val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                handleImageSelection(it)
            }
        }

    private val CAMERA_REQUEST_CODE = 123

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    private fun requestCameraPermission() {
        requestPermissions(
            arrayOf(android.Manifest.permission.CAMERA),
            CAMERA_REQUEST_CODE
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        img = findViewById(R.id.img)
        val gallary = findViewById<ImageView>(R.id.gallary)
        val camera = findViewById<ImageView>(R.id.camera)
        val button = findViewById<Button>(R.id.button)

        gallary.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        camera.setOnClickListener {
            if (checkCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        button.setOnClickListener {
            selectedImageUri.let { addWatermarkAndSave(it) }
        }
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap?
            imageBitmap?.let {
                img.setImageBitmap(it)
                handleImageSelection(saveImageToExternalStorage(it))
            }
        }
    }

    private fun handleImageSelection(uri: Uri) {

        selectedImageUri = uri

        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            img.setImageBitmap(bitmap)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun addWatermarkAndSave(imageUri: Uri) {
        try {
            val originalBitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            val watermarkedBitmap = Bitmap.createBitmap(
                originalBitmap.width,
                originalBitmap.height,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(watermarkedBitmap)
            canvas.drawBitmap(originalBitmap, 0f, 0f, null)

            val watermarkText = "@Raxa Chauhan"
            val paint = Paint().apply {
                color = resources.getColor(android.R.color.white)
                textSize = originalBitmap.width * 0.05f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val x = 50f
            val y = canvas.height - 50f

            canvas.drawText(watermarkText, x, y, paint)
            img.setImageBitmap(watermarkedBitmap)
            val watermarkedImageFile = saveWatermarkedImage(watermarkedBitmap)
            Toast.makeText(
                this,
                "Watermarked image saved to: ${watermarkedImageFile.absolutePath}",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    private fun saveImageToExternalStorage(bitmap: Bitmap): Uri {
        val timeStamp: String = sdf.format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)

        try {
            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return Uri.fromFile(imageFile)
    }

    private fun saveWatermarkedImage(bitmap: Bitmap): File {
        val timeStamp: String = sdf.format(Date())
        val imageFileName = "Watermarked_$timeStamp.jpg"
        val storageDir: File =
            File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "WatermarkedImages")

        if (!storageDir.exists()) {
            storageDir.mkdir()
        }

        val watermarkedImageFile = File(storageDir, imageFileName)

        try {
            FileOutputStream(watermarkedImageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return watermarkedImageFile
    }
}