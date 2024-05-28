package com.example.instafilterapp
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaScannerConnection
import android.content.Intent
import android.widget.Toast
import android.os.Environment
import android.widget.ImageView
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import androidx.core.content.FileProvider
import android.net.Uri

object ImageUtils {

    fun saveImageToGallery(context: Context, imageView: ImageView, filename: String): Boolean {
        // Obtener la imagen del ImageView
        val bitmap = (imageView.drawable as BitmapDrawable).bitmap

        // Crear la ruta del directorio de almacenamiento externo
        val storageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "InstaFilter")

        // Verificar si el directorio existe, si no, crearlo
        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                return false // Fallo al crear el directorio
            }
        }

        // Guardar la imagen en la memoria del dispositivo
        val imageFile = File(storageDir, filename)
        return try {
            val outputStream: OutputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            // Escanear manualmente el archivo para que aparezca en la galería
            scanFile(context, imageFile.absolutePath, "image/png")

            true // Éxito al guardar la imagen
        } catch (e: Exception) {
            e.printStackTrace()
            false // Fallo al guardar la imagen
        }
    }

    private fun scanFile(context: Context, path: String, mimeType: String) {
        MediaScannerConnection.scanFile(context, arrayOf(path), arrayOf(mimeType), null)
    }

    fun shareImageFromGallery(context: Context, imagePath: String) {
        val imageFile = File(imagePath)
        if (imageFile.exists()) {
            val imageUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                // Agregar tipos de datos adicionales para aplicaciones específicas
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/vnd.whatsapp", "text/plain", "text/*", "image/jpeg", "image/png"))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
        } else {
            Toast.makeText(context, "Image not found", Toast.LENGTH_SHORT).show()
        }
    }
}