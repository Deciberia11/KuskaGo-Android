package pe.kuskago.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.util.UUID

/** Copia reducida en almacenamiento privado; no requiere acceso permanente a la galería. */
object ProfilePhotos {
    fun save(context: Context, dni: String, uri: Uri): String? {
        val resolver = context.contentResolver
        val output = File(context.filesDir, "profile_${UUID.randomUUID()}.jpg")
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            require(options.outWidth > 0 && options.outHeight > 0)
            var sample = 1
            while (maxOf(options.outWidth, options.outHeight) / sample > 1024) sample *= 2
            options.inJustDecodeBounds = false
            options.inSampleSize = sample
            val source = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return null
            val orientation = resolver.openInputStream(uri)?.use {
                runCatching { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
            val transform = Matrix().apply {
                when (orientation) {
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                    ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(270f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
                }
            }
            val bitmap = Bitmap.createBitmap(source, 0, 0, source.width, source.height, transform, true)
            try {
                output.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it)) }
            } finally {
                if (bitmap !== source) bitmap.recycle()
                source.recycle()
            }
            val prefs = context.getSharedPreferences("kuskago_settings", Context.MODE_PRIVATE)
            val old = prefs.getString("profile_photo_$dni", null)
            check(prefs.edit().putString("profile_photo_$dni", output.absolutePath).commit())
            old?.let { File(it) }?.takeIf { it.parentFile == context.filesDir && it.name.startsWith("profile_") }?.delete()
            output.absolutePath
        } catch (_: Exception) {
            output.delete()
            null
        }
    }
}
