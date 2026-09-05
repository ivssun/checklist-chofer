package com.example.checklistchofer

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.example.checklistchofer.data.FirebaseRepository
import com.example.checklistchofer.ui.screens.ChecklistScreen
import com.example.checklistchofer.ui.screens.ControlViajeScreen
import com.example.checklistchofer.ui.screens.ViajeScreen
import com.example.checklistchofer.ui.theme.ChecklistChoferTheme
import kotlinx.coroutines.launch

sealed class Pantalla {
    object Inicio : Pantalla()
    data class Checklist(val viajeId: String) : Pantalla()
    data class ControlViaje(val viajeId: String) : Pantalla()
    data class RetomarViaje(val viajeId: String, val pantallaGuardada: String) : Pantalla()
}

// Persiste qué viaje/pantalla estaba en curso, para no perder el progreso si
// se cierra la app (el viaje ya existe en Firestore desde que se crea en ViajeScreen).
private const val PREFS_SESION = "checklist_sesion"
private const val KEY_VIAJE_ID = "viaje_id"
private const val KEY_PANTALLA = "pantalla"
private const val VALOR_CHECKLIST = "checklist"
private const val VALOR_CONTROL = "control"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences(PREFS_SESION, Context.MODE_PRIVATE)

        fun guardarSesion(pantalla: Pantalla) {
            when (pantalla) {
                is Pantalla.Checklist -> prefs.edit {
                    putString(KEY_VIAJE_ID, pantalla.viajeId)
                    putString(KEY_PANTALLA, VALOR_CHECKLIST)
                }
                is Pantalla.ControlViaje -> prefs.edit {
                    putString(KEY_VIAJE_ID, pantalla.viajeId)
                    putString(KEY_PANTALLA, VALOR_CONTROL)
                }
                is Pantalla.RetomarViaje -> Unit // no aplica, es una pantalla transitoria
                Pantalla.Inicio -> prefs.edit { clear() }
            }
        }

        // Si había un viaje en curso, no se retoma directo: se le pregunta al
        // chofer, porque puede que le hayan cancelado el viaje o cambiado de
        // unidad mientras la app estaba cerrada.
        val viajeIdGuardado = prefs.getString(KEY_VIAJE_ID, null)
        val pantallaGuardada = prefs.getString(KEY_PANTALLA, null)
        val pantallaInicial: Pantalla = if (viajeIdGuardado != null && pantallaGuardada != null) {
            Pantalla.RetomarViaje(viajeIdGuardado, pantallaGuardada)
        } else {
            Pantalla.Inicio
        }

        val screenState = mutableStateOf(pantallaInicial)

        setContent {
            ChecklistChoferTheme {
                when (val currentScreen = screenState.value) {
                    is Pantalla.Inicio -> ViajeScreen(
                        onViajeCreado = { viajeId ->
                            val siguiente = Pantalla.Checklist(viajeId)
                            screenState.value = siguiente
                            guardarSesion(siguiente)
                        }
                    )
                    is Pantalla.Checklist -> ChecklistScreen(
                        viajeId = currentScreen.viajeId,
                        onChecklistGuardado = {
                            val siguiente = Pantalla.ControlViaje(currentScreen.viajeId)
                            screenState.value = siguiente
                            guardarSesion(siguiente)
                        },
                        onViajeNoEncontrado = {
                            screenState.value = Pantalla.Inicio
                            guardarSesion(Pantalla.Inicio)
                        }
                    )
                    is Pantalla.ControlViaje -> ControlViajeScreen(
                        viajeId = currentScreen.viajeId,
                        onFinalizarViaje = {
                            screenState.value = Pantalla.Inicio
                            guardarSesion(Pantalla.Inicio)
                        },
                        onViajeNoEncontrado = {
                            screenState.value = Pantalla.Inicio
                            guardarSesion(Pantalla.Inicio)
                        }
                    )
                    is Pantalla.RetomarViaje -> RetomarViajeScreen(
                        viajeId = currentScreen.viajeId,
                        onContinuar = {
                            screenState.value = if (currentScreen.pantallaGuardada == VALOR_CONTROL) {
                                Pantalla.ControlViaje(currentScreen.viajeId)
                            } else {
                                Pantalla.Checklist(currentScreen.viajeId)
                            }
                        },
                        onNuevoViaje = {
                            screenState.value = Pantalla.Inicio
                            guardarSesion(Pantalla.Inicio)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetomarViajeScreen(
    viajeId: String,
    onContinuar: () -> Unit,
    onNuevoViaje: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val repository = remember { FirebaseRepository() }
    var cancelando by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Viaje en curso") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Tienes un checklist de viaje sin terminar.",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "¿Quieres continuar con ese viaje, o cancelarlo e iniciar uno nuevo (por ejemplo, si te cambiaron de unidad o se canceló el viaje)?",
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onContinuar,
                enabled = !cancelando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continuar con el viaje en curso")
            }
            OutlinedButton(
                onClick = {
                    cancelando = true
                    scope.launch {
                        try {
                            repository.cancelarViaje(viajeId)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            onNuevoViaje()
                        }
                    }
                },
                enabled = !cancelando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (cancelando) "Cancelando..." else "Cancelar e iniciar un nuevo viaje")
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
