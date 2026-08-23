package com.example.checklistchofer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.checklistchofer.data.Destino
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

private fun formatFecha(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "MX"))
    return formato.format(timestamp.toDate())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlViajeScreen(
    viajeId: String,
    viewModel: ControlViajeViewModel = remember(viajeId) { ControlViajeViewModel(viajeId) },
    onFinalizarViaje: () -> Unit = {}
) {
    val viaje by viewModel.viaje.collectAsState()
    val destinos by viewModel.destinos.collectAsState()
    val cargasCombustible by viewModel.cargasCombustible.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarError()
        }
    }

    var mostrarDialogoIniciar by remember { mutableStateOf<Destino?>(null) }
    var mostrarDialogoLlegada by remember { mutableStateOf<Destino?>(null) }
    var mostrarDialogoCombustible by remember { mutableStateOf(false) }

    val destinoActual = destinos.firstOrNull { it.fechaLlegada == null }
    val concluido = viaje?.concluido == true

    Scaffold(
        topBar = { TopAppBar(title = { Text("Bitácora de ruta por destino") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (isLoading && viaje == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                viaje?.let {
                    Text("Placa: ${it.placa}", fontWeight = FontWeight.SemiBold)
                    Text("No. de unidad / económico: ${it.economico}")
                }

                Text("Itinerario", fontWeight = FontWeight.Bold)
                destinos.forEach { destino ->
                    ItinerarioItem(destino)
                    HorizontalDivider()
                }

                if (concluido) {
                    Text("✅ Viaje concluido", fontWeight = FontWeight.Bold)
                    Button(
                        onClick = onFinalizarViaje,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Finalizar y volver al inicio")
                    }
                } else if (destinoActual != null) {
                    if (destinoActual.fechaSalida == null) {
                        Button(
                            onClick = { mostrarDialogoIniciar = destinoActual },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Iniciar viaje a ${destinoActual.cedisDestino}")
                        }
                    } else {
                        Button(
                            onClick = { mostrarDialogoLlegada = destinoActual },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ya llegué a ${destinoActual.cedisDestino}")
                        }
                    }
                }

                OutlinedButton(
                    onClick = { mostrarDialogoCombustible = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Agregar carga de combustible")
                }

                if (cargasCombustible.isNotEmpty()) {
                    Text("Cargas registradas", fontWeight = FontWeight.Bold)
                    cargasCombustible.forEach { carga ->
                        Text("${formatFecha(carga.fechaCarga)} — ${carga.ubicacion} — ${carga.litros} L @ $${carga.costoPorLitro}/L (km ${carga.kilometraje})")
                    }
                }
            }
        }
    }

    mostrarDialogoIniciar?.let { destino ->
        DialogoIniciarDestino(
            destino = destino,
            onDismiss = { mostrarDialogoIniciar = null },
            onConfirmar = { km ->
                viewModel.iniciarDestino(destino, km)
                mostrarDialogoIniciar = null
            }
        )
    }

    mostrarDialogoLlegada?.let { destino ->
        DialogoLlegadaDestino(
            viajeId = viajeId,
            destino = destino,
            onDismiss = { mostrarDialogoLlegada = null },
            onConfirmar = { kmFinal, entregadas, regresadas, fotoURL ->
                viewModel.registrarLlegada(destino, kmFinal, entregadas, regresadas, fotoURL)
                mostrarDialogoLlegada = null
            }
        )
    }

    if (mostrarDialogoCombustible) {
        DialogoCargaCombustible(
            onDismiss = { mostrarDialogoCombustible = false },
            onConfirmar = { ubicacion, km, costo, litros ->
                viewModel.agregarCargaCombustible(ubicacion, km, costo, litros)
                mostrarDialogoCombustible = false
            }
        )
    }
}

@Composable
fun ItinerarioItem(destino: Destino) {
    val estado = when {
        destino.fechaLlegada != null -> "Completado (Km ${destino.kmInicial} → ${destino.kmFinal})"
        destino.fechaSalida != null -> "En curso (salió Km ${destino.kmInicial})"
        else -> "Pendiente"
    }
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("${destino.orden + 1}. ${destino.cedisDestino}", fontWeight = FontWeight.SemiBold)
        Text(estado)
        if (destino.fechaSalida != null) {
            Text("Salida: ${formatFecha(destino.fechaSalida)}")
        }
        if (destino.fechaLlegada != null) {
            Text("Llegada: ${formatFecha(destino.fechaLlegada)}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoIniciarDestino(
    destino: Destino,
    onDismiss: () -> Unit,
    onConfirmar: (Int) -> Unit
) {
    var kmInicial by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Iniciar viaje a ${destino.cedisDestino}") },
        text = {
            OutlinedTextField(
                value = kmInicial,
                onValueChange = { kmInicial = it.filter { c -> c.isDigit() } },
                label = { Text("Km inicial") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { kmInicial.toIntOrNull()?.let { onConfirmar(it) } },
                enabled = kmInicial.toIntOrNull() != null
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoLlegadaDestino(
    viajeId: String,
    destino: Destino,
    onDismiss: () -> Unit,
    onConfirmar: (Int, Int, Int, String?) -> Unit
) {
    var kmFinal by remember { mutableStateOf("") }
    var canastillasEntregadas by remember { mutableStateOf("") }
    var canastillasRegresadas by remember { mutableStateOf("") }
    var fotoURL by remember { mutableStateOf<String?>(destino.fotoURL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ya llegué a ${destino.cedisDestino}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = kmFinal,
                    onValueChange = { kmFinal = it.filter { c -> c.isDigit() } },
                    label = { Text("Km final") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = canastillasEntregadas,
                    onValueChange = { canastillasEntregadas = it.filter { c -> c.isDigit() } },
                    label = { Text("Canastillas entregadas") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = canastillasRegresadas,
                    onValueChange = { canastillasRegresadas = it.filter { c -> c.isDigit() } },
                    label = { Text("Canastillas regresadas") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                BotonFotoCamara(
                    storagePath = "viajes/$viajeId/destinos/${destino.id}.jpg",
                    fotoURL = fotoURL,
                    onFotoSubida = { fotoURL = it }
                )
            }
        },
        confirmButton = {
            val kmFinalVal = kmFinal.toIntOrNull()
            val entregadasVal = canastillasEntregadas.toIntOrNull()
            val regresadasVal = canastillasRegresadas.toIntOrNull()
            Button(
                onClick = {
                    if (kmFinalVal != null && entregadasVal != null && regresadasVal != null) {
                        onConfirmar(kmFinalVal, entregadasVal, regresadasVal, fotoURL)
                    }
                },
                enabled = kmFinalVal != null && entregadasVal != null && regresadasVal != null
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoCargaCombustible(
    onDismiss: () -> Unit,
    onConfirmar: (String, Int, Float, Float) -> Unit
) {
    var ubicacion by remember { mutableStateOf("") }
    var kilometraje by remember { mutableStateOf("") }
    var costoPorLitro by remember { mutableStateOf("") }
    var litros by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar carga de combustible") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = ubicacion,
                    onValueChange = { ubicacion = it },
                    label = { Text("Ubicación") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = kilometraje,
                    onValueChange = { kilometraje = it.filter { c -> c.isDigit() } },
                    label = { Text("Kilometraje al cargar") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = costoPorLitro,
                    onValueChange = { costoPorLitro = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Costo por litro") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = litros,
                    onValueChange = { litros = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Litros cargados") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val kmVal = kilometraje.toIntOrNull()
            val costoVal = costoPorLitro.toFloatOrNull()
            val litrosVal = litros.toFloatOrNull()
            Button(
                onClick = {
                    if (ubicacion.isNotBlank() && kmVal != null && costoVal != null && litrosVal != null) {
                        onConfirmar(ubicacion, kmVal, costoVal, litrosVal)
                    }
                },
                enabled = ubicacion.isNotBlank() && kmVal != null && costoVal != null && litrosVal != null
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
