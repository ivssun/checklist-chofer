package com.example.checklistchofer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.checklistchofer.data.Camion
import com.example.checklistchofer.data.Chofer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViajeScreen(
    viewModel: ViajeViewModel = remember { ViajeViewModel() },
    onViajeCreado: (String) -> Unit = {}
) {
    val choferes by viewModel.choferes.collectAsState()
    val camionesFiltered by viewModel.camionesFiltered.collectAsState()
    val choferSeleccionado by viewModel.choferSeleccionado.collectAsState()
    val camionSeleccionado by viewModel.camionSeleccionado.collectAsState()
    val tipoUnidad by viewModel.tipoUnidad.collectAsState()
    val placaManual by viewModel.placaManual.collectAsState()
    val usarPlacaOtro by viewModel.usarPlacaOtro.collectAsState()
    val detalleRenta by viewModel.detalleRenta.collectAsState()
    val usarDetalleRentaOtro by viewModel.usarDetalleRentaOtro.collectAsState()
    val detalleRentaManual by viewModel.detalleRentaManual.collectAsState()
    val placaRentaSeleccionada by viewModel.placaRentaSeleccionada.collectAsState()
    val economicoManual by viewModel.economicoManual.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val viajeCreado by viewModel.viajeCreado.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val fechaFormato = SimpleDateFormat("dd/MM/yyyy", Locale("es", "MX"))
    val fechaActual = fechaFormato.format(Date())
    val tiposUnidad = listOf("GDE", "MED", "RENTA", "Otro")
    val marcasRenta = viewModel.getMarcasRenta()

    // Mostrar error en snackbar
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarError()
        }
    }

    // Navegar cuando viaje fue creado
    LaunchedEffect(viajeCreado) {
        viajeCreado?.let {
            onViajeCreado(it)
            viewModel.limpiarViajeCreado()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checklist Diario de Chofer") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Fecha automática (no editable)
                Text("Fecha: $fechaActual")

                // Sección Nombre
                Text("Nombre del colaborador", modifier = Modifier.padding(top = 8.dp))
                DropdownSelector(
                    label = "nombre",
                    selectedItem = choferSeleccionado,
                    items = choferes,
                    itemLabel = { it.nombre },
                    onItemSelected = { viewModel.seleccionarChofer(it) }
                )

                // Sección Tipo de Unidad
                Text("Tipo de Unidad", modifier = Modifier.padding(top = 16.dp))
                DropdownSelector(
                    label = "tipo de unidad",
                    selectedItem = tipoUnidad.ifEmpty { null },
                    items = tiposUnidad,
                    itemLabel = { it },
                    onItemSelected = { viewModel.cambiarTipoUnidad(it) }
                )

                // Sección Placas
                Text("Placas", modifier = Modifier.padding(top = 16.dp))

                when (tipoUnidad) {
                    "RENTA" -> {
                        // Selector de placa RENTA (con opción "Otro")
                        DropdownSelectorConOtro(
                            label = "placas",
                            selectedItem = placaRentaSeleccionada.ifEmpty { null },
                            items = viewModel.getPlacasRenta(),
                            itemLabel = { it },
                            onItemSelected = { viewModel.seleccionarPlacaRenta(it) },
                            usandoOtro = usarPlacaOtro,
                            valorManual = placaManual,
                            onSeleccionarOtro = { viewModel.seleccionarPlacaOtro() },
                            onValorManualChange = { viewModel.actualizarPlacaManual(it) }
                        )
                    }
                    "Otro" -> {
                        // Campo de placa manual
                        OutlinedTextField(
                            value = placaManual,
                            onValueChange = { viewModel.actualizarPlacaManual(it) },
                            placeholder = { Text("Ingresar dato") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    else -> {
                        // Selector de Camión para GDE/MED (con opción "Otro")
                        DropdownSelectorConOtro(
                            label = "placa",
                            selectedItem = camionSeleccionado,
                            items = camionesFiltered,
                            itemLabel = { it.placa },
                            onItemSelected = { viewModel.seleccionarCamion(it) },
                            usandoOtro = usarPlacaOtro,
                            valorManual = placaManual,
                            onSeleccionarOtro = { viewModel.seleccionarPlacaOtro() },
                            onValorManualChange = { viewModel.actualizarPlacaManual(it) }
                        )
                    }
                }

                // Sección Detalle Renta (solo si es RENTA)
                if (tipoUnidad == "RENTA") {
                    Text("Detalle Renta", modifier = Modifier.padding(top = 16.dp))
                    DropdownSelectorConOtro(
                        label = "detalle",
                        selectedItem = detalleRenta.ifEmpty { null },
                        items = marcasRenta,
                        itemLabel = { it },
                        onItemSelected = { viewModel.seleccionarDetalleRenta(it) },
                        usandoOtro = usarDetalleRentaOtro,
                        valorManual = detalleRentaManual,
                        onSeleccionarOtro = { viewModel.seleccionarDetalleRentaOtro() },
                        onValorManualChange = { viewModel.actualizarDetalleRentaManual(it) }
                    )
                }

                // Sección No. de Unidad / Económico (ingreso manual para todos)
                Text("No. de Unidad / Económico", modifier = Modifier.padding(top = 16.dp))
                OutlinedTextField(
                    value = economicoManual,
                    onValueChange = { viewModel.actualizarEconomicoManual(it) },
                    placeholder = { Text("Ingresar dato") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                // Info del camión si está seleccionado (solo para GDE/MED)
                if (tipoUnidad in listOf("GDE", "MED") && camionSeleccionado != null) {
                    CamionInfoCard(camion = camionSeleccionado!!)
                }

                // Botón Iniciar Viaje
                Button(
                    onClick = { viewModel.iniciarViaje() },
                    enabled = choferSeleccionado != null && economicoManual.isNotEmpty() && when (tipoUnidad) {
                        "RENTA" ->
                            (if (usarPlacaOtro) placaManual.isNotEmpty() else placaRentaSeleccionada.isNotEmpty()) &&
                            (if (usarDetalleRentaOtro) detalleRentaManual.isNotEmpty() else detalleRenta.isNotEmpty())
                        "Otro" -> placaManual.isNotEmpty()
                        else -> if (usarPlacaOtro) placaManual.isNotEmpty() else camionSeleccionado != null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Iniciar Viaje")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownSelector(
    label: String,
    selectedItem: T?,
    items: List<T>,
    itemLabel: (T) -> String,
    onItemSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = selectedItem?.let { itemLabel(it) } ?: "Seleccionar $label",
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemLabel(item)) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownSelectorConOtro(
    label: String,
    selectedItem: T?,
    items: List<T>,
    itemLabel: (T) -> String,
    onItemSelected: (T) -> Unit,
    usandoOtro: Boolean,
    valorManual: String,
    onSeleccionarOtro: () -> Unit,
    onValorManualChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = when {
                    usandoOtro -> "Otro"
                    selectedItem != null -> itemLabel(selectedItem)
                    else -> "Seleccionar $label"
                },
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemLabel(item)) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Otro") },
                onClick = {
                    onSeleccionarOtro()
                    expanded = false
                }
            )
        }

        if (usandoOtro) {
            OutlinedTextField(
                value = valorManual,
                onValueChange = onValorManualChange,
                placeholder = { Text("Ingresar dato") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@Composable
fun CamionInfoCard(camion: Camion) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text("Información del Camión")
        Text("Placa: ${camion.placa}")
        Text("Tipo: ${camion.tipo}")
        Text("KM último servicio: ${camion.kilometrajeUltimoServicio}")
        Text("Posiciones de llantas: ${camion.posicionesLlantas.size}")
    }
}
