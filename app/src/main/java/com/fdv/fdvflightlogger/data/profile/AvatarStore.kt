package com.fdv.fdvflightlogger.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.min

/** The pilot's photo: one small square JPEG in the app's private storage. */
object AvatarStore {

    private const val FILE_NAME = "avatar.jpg"
    private const val SIZE_PX = 256      // plenty for a 56 dp circle on any screen
    private const val JPEG_QUALITY = 88

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    suspend fun load(context: Context): Bitmap? = withContext(Dispatchers.IO) {
        file(context).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
    }

    /** Crops the picked image to a centred square, shrinks it, saves it and returns it. */
    suspend fun saveFrom(context: Context, uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            // Decode at about twice the final size, not the camera's full 12 MP
            val shortSide = min(info.size.width, info.size.height)
            if (shortSide > SIZE_PX * 2) {
                val scale = (SIZE_PX * 2).toFloat() / shortSide
                decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
            }
            // Hardware bitmaps live on the GPU and can't be cropped or compressed
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val square = centreSquare(decoded)
        val bytes = ByteArrayOutputStream().use { out ->
            square.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
        writeAtomically(context, bytes)
        square
    }

    /** The saved JPEG, for backups; null when there's no photo. */
    suspend fun jpegBytes(context: Context): ByteArray? = withContext(Dispatchers.IO) {
        file(context).takeIf { it.exists() }?.readBytes()
    }

    /** Saves a photo from a backup; null if the bytes aren't a readable image. */
    suspend fun restore(context: Context, jpeg: ByteArray): Bitmap? = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) ?: return@withContext null
        writeAtomically(context, jpeg)
        bitmap
    }

    suspend fun delete(context: Context) {
        withContext(Dispatchers.IO) { file(context).delete() }
    }

    private fun centreSquare(src: Bitmap): Bitmap {
        val side = min(src.width, src.height)
        val cropped = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
        return Bitmap.createScaledBitmap(cropped, SIZE_PX, SIZE_PX, true)
    }

    // Write to a temporary file, then rename: a crash mid-write never leaves a broken avatar
    private fun writeAtomically(context: Context, bytes: ByteArray) {
        val tmp = File(context.filesDir, "$FILE_NAME.tmp")
        tmp.writeBytes(bytes)
        tmp.renameTo(file(context))
    }
}