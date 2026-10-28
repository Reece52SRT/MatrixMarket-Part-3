package com.matrixmarket.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

// Prepares a listing photo: decodes it, fixes camera rotation, shrinks it to at most
// 1280px and saves a JPEG copy inside the app's own storage. Keeping our own copy
// means a listing created OFFLINE can still upload its photo later during sync.
object ImageUtils {
    private const val TAG = "ImageUtils"
    private const val MAX_SIDE = 1280

    fun prepareListingPhoto(context: Context, source: Uri): File? {
        return try {
            val resolver = context.contentResolver

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (bounds.outWidth / (sample * 2) >= MAX_SIDE || bounds.outHeight / (sample * 2) >= MAX_SIDE) {
                sample *= 2
            }

            val decoded = resolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return null

            val rotation = resolver.openInputStream(source)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f

            val scaled = scaleDown(decoded)
            val finalBitmap = if (rotation != 0f) {
                val matrix = Matrix().apply { postRotate(rotation) }
                Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true)
            } else {
                scaled
            }

            val dir = File(context.filesDir, "listing_photos").apply { mkdirs() }
            val out = File(dir, "${UUID.randomUUID()}.jpg")
            FileOutputStream(out).use { finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            Log.i(TAG, "Prepared photo ${out.name} (${out.length() / 1024} KB)")
            out
        } catch (e: Exception) {
            Log.e(TAG, "Could not prepare photo", e)
            null
        }
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= MAX_SIDE) return bitmap
        val ratio = MAX_SIDE.toFloat() / longest
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
    }

    // Creates an empty file the camera app can write a new photo into.
    fun newCameraFile(context: Context): File {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        return File(dir, "capture_${System.currentTimeMillis()}.jpg")
    }
}
