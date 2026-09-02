package com.example.checklistchofer.ui.screens

import android.content.Context
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.example.checklistchofer.data.Camion
import com.example.checklistchofer.data.Chofer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViajeScreen(
    viewModel: ViajeViewModel = run {
        val context = LocalContext.current
        val prefsBorrador = remember {
            context.getSharedPreferences("checklist_borrador", Context.MODE_PRIVATE)
        }
        remember { ViajeViewModel(prefs = prefsBorrador) }
    },
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
    val tipoUnidadLabels = mapOf(
        "GDE" to "ISUZU GDE",
        "MED" to "ISUZU MED",
        "RENTA" to "ISUZU RENTA",
        "Otro" to "OTRO"
    )
    val marcasRenta = viewModel.getMarcasRenta()

    // Navegación automática entre campos: cada señal se incrementa para
    // forzar que el siguiente dropdown se abra solo, y cada FocusRequester
    // se usa para saltar el teclado al siguiente campo de texto.
    var abrirTipoSignal by remember { mutableStateOf(0) }
    var abrirDetalleRentaSignal by remember { mutableStateOf(0) }
    var abrirPlacasRentaSignal by remember { mutableStateOf(0) }
    var abrirPlacaCamionSignal by remember { mutableStateOf(0) }
    val marcaOtroFocus = remember { FocusRequester() }
    val placaOtroFocus = remember { FocusRequester() }
    val economicoFocus = remember { FocusRequester() }

    LaunchedEffect(choferSeleccionado) {
        if (choferSeleccionado != null) abrirTipoSignal++
    }
    LaunchedEffect(tipoUnidad) {
        when (tipoUnidad) {
            "RENTA" -> abrirDetalleRentaSignal++
            "GDE", "MED" -> abrirPlacaCamionSignal++
            "Otro" -> marcaOtroFocus.requestFocus()
        }
    }
    LaunchedEffect(detalleRenta) {
        if (detalleRenta.isNotEmpty()) abrirPlacasRentaSignal++
    }
    LaunchedEffect(placaRentaSeleccionada) {
        if (placaRentaSeleccionada.isNotEmpty()) economicoFocus.requestFocus()
    }
    LaunchedEffect(camionSeleccionado) {
        if (camionSeleccionado != null) economicoFocus.requestFocus()
    }

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
                DropdownSelectorFiltro(
                    label = "nombre",
                    selectedItem = choferSeleccionado,
                    items = choferes,
                    onItemSelected = { viewModel.seleccionarChofer(it) }
                )

                // Sección Tipo de Unidad
                Text("Tipo de Unidad", modifier = Modifier.padding(top = 16.dp))
                DropdownSelector(
                    label = "tipo de unidad",
                    selectedItem = tipoUnidad.ifEmpty { null },
                    items = tiposUnidad,
                    itemLabel = { tipoUnidadLabels[it] ?: it },
                    onItemSelected = { viewModel.cambiarTipoUnidad(it) },
                    autoAbrirSignal = abrirTipoSignal
                )

                when (tipoUnidad) {
                    "RENTA" -> {
                        // Primero Detalle Renta (marca), luego Placas
                        Text("Detalle Renta", modifier = Modifier.padding(top = 16.dp))
                        DropdownSelectorConOtro(
                            label = "dato",
                            selectedItem = detalleRenta.ifEmpty { null },
                            items = marcasRenta,
                            itemLabel = { it },
                            onItemSelected = { viewModel.seleccionarDetalleRenta(it) },
                            usandoOtro = usarDetalleRentaOtro,
                            valorManual = detalleRentaManual,
                            onSeleccionarOtro = { viewModel.seleccionarDetalleRentaOtro() },
                            onValorManualChange = { viewModel.actualizarDetalleRentaManual(it) },
                            autoAbrirSignal = abrirDetalleRentaSignal,
                            onListo = { abrirPlacasRentaSignal++ }
                        )

                        Text("Placas", modifier = Modifier.padding(top = 16.dp))
                        DropdownSelectorConOtro(
                            label = "placas",
                            selectedItem = placaRentaSeleccionada.ifEmpty { null },
                            items = viewModel.getPlacasRenta(),
                            itemLabel = { it },
                            onItemSelected = { viewModel.seleccionarPlacaRenta(it) },
                            usandoOtro = usarPlacaOtro,
                            valorManual = placaManual,
                            onSeleccionarOtro = { viewModel.seleccionarPlacaOtro() },
                            onValorManualChange = { viewModel.actualizarPlacaManual(it) },
                            autoAbrirSignal = abrirPlacasRentaSignal,
                            onListo = { economicoFocus.requestFocus() }
                        )
                    }
                    "Otro" -> {
                        // Marca y Placas, ambos manuales
                        Text("Marca", modifier = Modifier.padding(top = 16.dp))
                        OutlinedTextField(
                            value = detalleRentaManual,
                            onValueChange = { viewModel.actualizarDetalleRentaManual(it) },
                            placeholder = { Text("Ingresar marca", color = MaterialTheme.colorScheme.error) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { placaOtroFocus.requestFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(marcaOtroFocus)
                                .height(56.dp),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Text("Placas", modifier = Modifier.padding(top = 16.dp))
                        OutlinedTextField(
                            value = placaManual,
                            onValueChange = { viewModel.actualizarPlacaManual(it) },
                            placeholder = { Text("Ingresar placa", color = MaterialTheme.colorScheme.error) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { economicoFocus.requestFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(placaOtroFocus)
                                .height(56.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    else -> {
                        // Selector de Camión para GDE/MED (con opción "Otro")
                        Text("Placas", modifier = Modifier.padding(top = 16.dp))
                        DropdownSelectorConOtro(
                            label = "placa",
                            selectedItem = camionSeleccionado,
                            items = camionesFiltered,
                            itemLabel = { it.placa },
                            onItemSelected = { viewModel.seleccionarCamion(it) },
                            usandoOtro = usarPlacaOtro,
                            valorManual = placaManual,
                            onSeleccionarOtro = { viewModel.seleccionarPlacaOtro() },
                            onValorManualChange = { viewModel.actualizarPlacaManual(it) },
                            autoAbrirSignal = abrirPlacaCamionSignal,
                            onListo = { economicoFocus.requestFocus() }
                        )
                    }
                }

                // Sección No. de Unidad / Económico (ingreso manual para todos)
                Text("No. de Unidad / Económico", modifier = Modifier.padding(top = 16.dp))
                OutlinedTextField(
                    value = economicoManual,
                    onValueChange = { viewModel.actualizarEconomicoManual(it) },
                    placeholder = { Text("Ingresar dato", color = MaterialTheme.colorScheme.error) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(economicoFocus)
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
                        "Otro" -> placaManual.isNotEmpty() && detalleRentaManual.isNotEmpty()
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
    onItemSelected: (T) -> Unit,
    autoAbrirSignal: Int = 0
) {
    var expanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(autoAbrirSignal) {
        if (autoAbrirSignal > 0) {
            focusRequester.requestFocus()
            expanded = true
        }
    }

    Column {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .height(56.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = selectedItem?.let { itemLabel(it) } ?: "Seleccionar $label",
                color = if (selectedItem == null) MaterialTheme.colorScheme.error else Color.Unspecified,
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
fun DropdownSelectorFiltro(
    label: String,
    selectedItem: Chofer?,
    items: List<Chofer>,
    onItemSelected: (Chofer) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var texto by remember(selectedItem) { mutableStateOf(selectedItem?.nombre ?: "") }
    val itemsFiltrados = remember(texto, items) {
        if (texto.isBlank() || texto == selectedItem?.nombre) items
        else items.filter { it.nombre.contains(texto, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = true }
    ) {
        OutlinedTextField(
            value = texto,
            onValueChange = {
                texto = it
                expanded = true
            },
            placeholder = { Text("Escribir o seleccionar $label", color = MaterialTheme.colorScheme.error) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = false)
                .onFocusChanged { focusState -> if (focusState.isFocused) expanded = true }
                .height(56.dp),
            shape = RoundedCornerShape(8.dp)
        )

        DropdownMenu(
            expanded = expanded && itemsFiltrados.isNotEmpty(),
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = false),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsFiltrados.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.nombre) },
                    onClick = {
                        texto = item.nombre
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
    onValorManualChange: (String) -> Unit,
    autoAbrirSignal: Int = 0,
    onListo: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val manualFocusRequester = remember { FocusRequester() }

    LaunchedEffect(autoAbrirSignal) {
        if (autoAbrirSignal > 0) {
            focusRequester.requestFocus()
            expanded = true
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .height(56.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = when {
                    usandoOtro -> "Otro"
                    selectedItem != null -> itemLabel(selectedItem)
                    else -> "Seleccionar $label"
                },
                color = if (!usandoOtro && selectedItem == null) MaterialTheme.colorScheme.error else Color.Unspecified,
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

        LaunchedEffect(usandoOtro) {
            if (usandoOtro) manualFocusRequester.requestFocus()
        }

        if (usandoOtro) {
            OutlinedTextField(
                value = valorManual,
                onValueChange = onValorManualChange,
                placeholder = { Text("Ingresar dato", color = MaterialTheme.colorScheme.error) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { onListo() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(manualFocusRequester)
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
        Text(
            text = "Información del Camión",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text("Placa: ${camion.placa}")
        Text("Tipo: ${camion.tipo}")
        Text("KM último servicio: ${camion.kilometrajeUltimoServicio}")
    }
}
