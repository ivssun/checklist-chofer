package com.example.checklistchofer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.example.checklistchofer.ui.screens.ChecklistScreen
import com.example.checklistchofer.ui.screens.ControlViajeScreen
import com.example.checklistchofer.ui.screens.ViajeScreen
import com.example.checklistchofer.ui.theme.ChecklistChoferTheme

sealed class Pantalla {
    object Inicio : Pantalla()
    data class Checklist(val viajeId: String) : Pantalla()
    data class ControlViaje(val viajeId: String) : Pantalla()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val screenState = mutableStateOf<Pantalla>(Pantalla.Inicio)

        setContent {
            ChecklistChoferTheme {
                when (val currentScreen = screenState.value) {
                    is Pantalla.Inicio -> ViajeScreen(
                        onViajeCreado = { viajeId ->
                            screenState.value = Pantalla.Checklist(viajeId)
                        }
                    )
                    is Pantalla.Checklist -> ChecklistScreen(
                        viajeId = currentScreen.viajeId,
                        onChecklistGuardado = {
                            screenState.value = Pantalla.ControlViaje(currentScreen.viajeId)
                        }
                    )
                    is Pantalla.ControlViaje -> ControlViajeScreen(
                        viajeId = currentScreen.viajeId,
                        onFinalizarViaje = {
                            screenState.value = Pantalla.Inicio
                        }
                    )
                }
            }
        }
    }
}