package com.example.instafilterapp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat.getExternalFilesDirs
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object ImageUtils {

    fun saveImageToGallery(context: Context, bitmap: Bitmap, filename: String): Boolean {
        val storageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "InstaFilter"
        )

        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.e("ImageUtils", "Failed to create directory: $storageDir")
                return false
            }
        }

        val imageFile = File(storageDir, filename)
        return try {
            val outputStream: OutputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            MediaScannerConnection.scanFile(context, arrayOf(imageFile.absolutePath), null) { path, uri ->
                Log.d("ImageUtils", "File scanned: $path")
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ImageUtils", "Error saving image: ${e.message}")
            false
        }
    }
}
