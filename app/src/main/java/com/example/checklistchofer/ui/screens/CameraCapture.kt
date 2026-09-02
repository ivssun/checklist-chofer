package com.example.checklistchofer.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.checklistchofer.data.FirebaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val DIMENSION_MAXIMA_PX = 1280
private const val CALIDAD_JPEG = 75

private fun crearArchivoTemporalFoto(context: Context): File {
    val dir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
    return File(dir, "foto_${System.currentTimeMillis()}.jpg")
}

private fun uriParaArchivo(context: Context, archivo: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)

/**
 * Redimensiona (máximo [DIMENSION_MAXIMA_PX] px de lado mayor) y recomprime a JPEG
 * calidad [CALIDAD_JPEG] para no saturar Storage. Corrige la rotación según EXIF,
 * ya que al recomprimir con Bitmap se pierden los metadatos originales.
 */
private fun comprimirImagen(archivoOriginal: File): File {
    val orientacion = ExifInterface(archivoOriginal.absolutePath)
        .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(archivoOriginal.absolutePath, bounds)

    var sampleSize = 1
    while (bounds.outWidth / (sampleSize * 2) >= DIMENSION_MAXIMA_PX ||
        bounds.outHeight / (sampleSize * 2) >= DIMENSION_MAXIMA_PX
    ) {
        sampleSize *= 2
    }

    var bitmap = BitmapFactory.decodeFile(
        archivoOriginal.absolutePath,
        BitmapFactory.Options().apply { inSampleSize = sampleSize }
    ) ?: throw IllegalStateException("No se pudo leer la foto capturada")

    val escala = DIMENSION_MAXIMA_PX.toFloat() / maxOf(bitmap.width, bitmap.height)
    if (escala < 1f) {
        bitmap = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * escala).toInt(),
            (bitmap.height * escala).toInt(),
            true
        )
    }

    val matrix = Matrix()
    when (orientacion) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
    }
    if (!matrix.isIdentity) {
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    val archivoComprimido = File(archivoOriginal.parentFile, "cmp_${archivoOriginal.name}")
    FileOutputStream(archivoComprimido).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, out)
    }
    bitmap.recycle()

    return archivoComprimido
}

/**
 * Botón que toma una foto con la cámara (nunca galería), la comprime y la sube a
 * Firebase Storage bajo la ruta [storagePath]. Al terminar, invoca [onFotoSubida]
 * con la URL de descarga.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotonFotoCamara(
    storagePath: String,
    fotoURL: String?,
    onFotoSubida: (String) -> Unit,
    obligatoria: Boolean = false,
    repository: FirebaseRepository = remember { FirebaseRepository() }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tempFile by remember { mutableStateOf<File?>(null) }
    var subiendo by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun subirFoto(archivo: File) {
        subiendo = true
        scope.launch {
            try {
                val comprimido = withContext(Dispatchers.Default) { comprimirImagen(archivo) }
                val url = repository.subirFoto(uriParaArchivo(context, comprimido), storagePath)
                onFotoSubida(url)
                error = null
            } catch (e: Exception) {
                error = "Error al subir foto: ${e.message}"
            } finally {
                subiendo = false
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { exito ->
        if (exito) {
            tempFile?.let { subirFoto(it) }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            val archivo = crearArchivoTemporalFoto(context)
            tempFile = archivo
            cameraLauncher.launch(uriParaArchivo(context, archivo))
        } else {
            error = "Permiso de cámara denegado"
        }
    }

    Column {
        OutlinedButton(
            onClick = {
                val tienePermiso = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED

                if (tienePermiso) {
                    val archivo = crearArchivoTemporalFoto(context)
                    tempFile = archivo
                    cameraLauncher.launch(uriParaArchivo(context, archivo))
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            enabled = !subiendo
        ) {
            Text(
                when {
                    subiendo -> "Subiendo foto..."
                    !fotoURL.isNullOrEmpty() -> "📷 Foto tomada ✓"
                    obligatoria -> "📷 Tomar foto"
                    else -> "📷 Tomar foto (opcional)"
                }
            )
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
