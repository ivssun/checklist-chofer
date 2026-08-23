package com.example.checklistchofer.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import kotlinx.coroutines.launch
import java.io.File

private fun crearArchivoTemporalFoto(context: Context): Uri {
    val dir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
    val archivo = File(dir, "foto_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
}

/**
 * Botón que toma una foto con la cámara (nunca galería) y la sube a Firebase Storage
 * bajo la ruta [storagePath]. Al terminar, invoca [onFotoSubida] con la URL de descarga.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotonFotoCamara(
    storagePath: String,
    fotoURL: String?,
    onFotoSubida: (String) -> Unit,
    repository: FirebaseRepository = remember { FirebaseRepository() }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tempUri by remember { mutableStateOf<Uri?>(null) }
    var subiendo by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun subirFoto(uri: Uri) {
        subiendo = true
        scope.launch {
            try {
                val url = repository.subirFoto(uri, storagePath)
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
            tempUri?.let { subirFoto(it) }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            val uri = crearArchivoTemporalFoto(context)
            tempUri = uri
            cameraLauncher.launch(uri)
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
                    val uri = crearArchivoTemporalFoto(context)
                    tempUri = uri
                    cameraLauncher.launch(uri)
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
                    else -> "📷 Tomar foto (opcional)"
                }
            )
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
