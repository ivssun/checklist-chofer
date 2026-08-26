package com.example.checklistchofer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.checklistchofer.data.DestinoCatalogo
import com.example.checklistchofer.data.PresionLlanta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(
    viajeId: String,
    viewModel: ChecklistViewModel = remember(viajeId) { ChecklistViewModel(viajeId) },
    onChecklistGuardado: () -> Unit = {},
    onViajeNoEncontrado: () -> Unit = {}
) {
    val viaje by viewModel.viaje.collectAsState()
    val camion by viewModel.camion.collectAsState()
    val destinosCatalogo by viewModel.destinosCatalogo.collectAsState()
    val destinosSeleccionados by viewModel.destinosSeleccionados.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val checklistGuardado by viewModel.checklistGuardado.collectAsState()
    val formularioCompleto by viewModel.formularioCompleto.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarError()
        }
    }

    LaunchedEffect(checklistGuardado) {
        if (checklistGuardado) {
            onChecklistGuardado()
        }
    }

    // El viaje que se intentaba retomar ya no existe en Firestore (p. ej. se
    // borró desde la consola) — regresar a Pantalla 1 en vez de dejar la
    // pantalla en blanco sin salida.
    LaunchedEffect(isLoading, viaje) {
        if (!isLoading && viaje == null) {
            onViajeNoEncontrado()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checklist Viaje") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (viaje != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ========== ITINERARIO ==========
                SeccionTitulo("1. Itinerario del Viaje")

                SeccionItinerario(
                    destinosSeleccionados = destinosSeleccionados,
                    destinosCatalogo = destinosCatalogo,
                    onAgregarDestino = { viewModel.agregarDestino(it) },
                    onEliminarDestino = { viewModel.eliminarDestino(it) }
                )

                // ========== COMBUSTIBLE Y LIMPIEZA ==========
                SeccionTitulo("2. Combustible y Limpieza")

                CampoSiNo(
                    label = "¿Salió con tanque lleno diésel?",
                    valor = viaje!!.combustibleYLimpieza.tanqueLlenoSalida.valor,
                    observacion = viaje!!.combustibleYLimpieza.tanqueLlenoSalida.observacion,
                    onUpdate = { v, o -> viewModel.updateTanqueLlenoSalida(v, o) },
                    storagePath = "viajes/$viajeId/checklist/combustible_salida.jpg",
                    fotoURL = viaje!!.combustibleYLimpieza.tanqueLlenoSalida.fotoURL,
                    onFotoCapturada = { viewModel.updateFotoCombustible("salida", it) }
                )

                CampoSiNo(
                    label = "¿Regresó con tanque lleno?",
                    valor = viaje!!.combustibleYLimpieza.tanqueLlenoRegreso.valor,
                    observacion = viaje!!.combustibleYLimpieza.tanqueLlenoRegreso.observacion,
                    onUpdate = { v, o -> viewModel.updateTanqueLlenoRegreso(v, o) },
                    storagePath = "viajes/$viajeId/checklist/combustible_regreso.jpg",
                    fotoURL = viaje!!.combustibleYLimpieza.tanqueLlenoRegreso.fotoURL,
                    onFotoCapturada = { viewModel.updateFotoCombustible("regreso", it) }
                )

                CampoSiNo(
                    label = "¿Se limpió caja y cabina?",
                    valor = viaje!!.combustibleYLimpieza.limpiezaCajaCabina.valor,
                    observacion = viaje!!.combustibleYLimpieza.limpiezaCajaCabina.observacion,
                    onUpdate = { v, o -> viewModel.updateLimpiezaCajaCabina(v, o) },
                    storagePath = "viajes/$viajeId/checklist/combustible_limpieza.jpg",
                    fotoURL = viaje!!.combustibleYLimpieza.limpiezaCajaCabina.fotoURL,
                    onFotoCapturada = { viewModel.updateFotoCombustible("limpieza", it) }
                )

                // ========== INSPECCIÓN GENERAL ==========
                SeccionTitulo("3. Inspección General")

                CampoInspeccionSiNoNa("Llantas (desgaste)", viaje!!.inspeccionGeneral.llantasDesgaste, { v, o -> viewModel.updateInspeccion("1", v, o) }, "viajes/$viajeId/checklist/inspeccion_1.jpg", { viewModel.updateFotoInspeccion("1", it) })
                CampoInspeccionSiNoNa("Llanta de refacción", viaje!!.inspeccionGeneral.llantaRefaccion, { v, o -> viewModel.updateInspeccion("2", v, o) }, "viajes/$viajeId/checklist/inspeccion_2.jpg", { viewModel.updateFotoInspeccion("2", it) })

                Text("Presión de llantas (poner numero)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                SeccionPresionLlantas(
                    camion = camion,
                    presiones = viaje!!.presionLlantas,
                    onUpdate = { viewModel.updatePresionLlantas(it) }
                )
                SeccionObservacionFoto(
                    observacion = viaje!!.presionLlantasObservacion.observacion,
                    fotoURL = viaje!!.presionLlantasObservacion.fotoURL,
                    storagePath = "viajes/$viajeId/checklist/presion_llantas.jpg",
                    onObservacionChange = { viewModel.updatePresionLlantasObservacion(it) },
                    onFotoCapturada = { viewModel.updateFotoPresionLlantas(it) }
                )

                CampoInspeccionSiNoNa("Sistema de frenado óptimo", viaje!!.inspeccionGeneral.sistemaFrenado, { v, o -> viewModel.updateInspeccion("4", v, o) }, "viajes/$viajeId/checklist/inspeccion_4.jpg", { viewModel.updateFotoInspeccion("4", it) })
                CampoInspeccionSiNoNa("Luces (altas, bajas, direccionales, reversa, stop)", viaje!!.inspeccionGeneral.luces, { v, o -> viewModel.updateInspeccion("5", v, o) }, "viajes/$viajeId/checklist/inspeccion_5.jpg", { viewModel.updateFotoInspeccion("5", it) })
                CampoInspeccionSiNoNa("Espejos laterales y retrovisor", viaje!!.inspeccionGeneral.espejos, { v, o -> viewModel.updateInspeccion("6", v, o) }, "viajes/$viajeId/checklist/inspeccion_6.jpg", { viewModel.updateFotoInspeccion("6", it) })
                CampoInspeccionSiNoNa("Limpiaparabrisas y claxon", viaje!!.inspeccionGeneral.limpiaparabrisas, { v, o -> viewModel.updateInspeccion("7", v, o) }, "viajes/$viajeId/checklist/inspeccion_7.jpg", { viewModel.updateFotoInspeccion("7", it) })
                CampoInspeccionSiNoNa("Nivel de aceite de motor", viaje!!.inspeccionGeneral.nivelAceite, { v, o -> viewModel.updateInspeccion("8", v, o) }, "viajes/$viajeId/checklist/inspeccion_8.jpg", { viewModel.updateFotoInspeccion("8", it) })
                CampoInspeccionSiNoNa("Nivel de agua / refrigerante", viaje!!.inspeccionGeneral.nivelAgua, { v, o -> viewModel.updateInspeccion("9", v, o) }, "viajes/$viajeId/checklist/inspeccion_9.jpg", { viewModel.updateFotoInspeccion("9", it) })
                CampoInspeccionSiNoNa("Nivel de líquido de frenos (en caso de que aplique)", viaje!!.inspeccionGeneral.nivelLiquidoFreno, { v, o -> viewModel.updateInspeccion("10", v, o) }, "viajes/$viajeId/checklist/inspeccion_10.jpg", { viewModel.updateFotoInspeccion("10", it) })

                Text("Nivel de Urea", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                var ureaValor by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = ureaValor,
                    onValueChange = {
                        val filtered = it.filter { c -> c.isDigit() }
                        ureaValor = filtered
                        viewModel.updateUreaPorcentaje(filtered.toIntOrNull() ?: 0)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("%") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )
                SeccionObservacionFoto(
                    observacion = viaje!!.ureaObservacion.observacion,
                    fotoURL = viaje!!.ureaObservacion.fotoURL,
                    storagePath = "viajes/$viajeId/checklist/urea.jpg",
                    onObservacionChange = { viewModel.updateUreaObservacion(it) },
                    onFotoCapturada = { viewModel.updateFotoUrea(it) }
                )

                Text("Nivel de combustible para thermo", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                SeccionCombustibleThermo(
                    valor = viaje!!.combustibleThermo,
                    onUpdate = { viewModel.updateCombustibleThermo(it) }
                )
                SeccionObservacionFoto(
                    observacion = viaje!!.combustibleThermoObservacion.observacion,
                    fotoURL = viaje!!.combustibleThermoObservacion.fotoURL,
                    storagePath = "viajes/$viajeId/checklist/combustible_thermo.jpg",
                    onObservacionChange = { viewModel.updateCombustibleThermoObservacion(it) },
                    onFotoCapturada = { viewModel.updateFotoCombustibleThermo(it) }
                )

                CampoInspeccionSiNoNa("Batería (terminales y carga)", viaje!!.inspeccionGeneral.bateria, { v, o -> viewModel.updateInspeccion("13", v, o) }, "viajes/$viajeId/checklist/inspeccion_13.jpg", { viewModel.updateFotoInspeccion("13", it) })
                CampoInspeccionSiNoNa("Triángulos de seguridad / señalización", viaje!!.inspeccionGeneral.triangulos, { v, o -> viewModel.updateInspeccion("14", v, o) }, "viajes/$viajeId/checklist/inspeccion_14.jpg", { viewModel.updateFotoInspeccion("14", it) })
                CampoInspeccionSiNoNa("Gato hidráulico, cruceta y herramienta básica", viaje!!.inspeccionGeneral.gato, { v, o -> viewModel.updateInspeccion("15", v, o) }, "viajes/$viajeId/checklist/inspeccion_15.jpg", { viewModel.updateFotoInspeccion("15", it) })
                CampoInspeccionSiNoNa("Estado de Carrocería (golpes, rayones, abolladuras)", viaje!!.inspeccionGeneral.carroceria, { v, o -> viewModel.updateInspeccion("16", v, o) }, "viajes/$viajeId/checklist/inspeccion_16.jpg", { viewModel.updateFotoInspeccion("16", it) })
                CampoInspeccionSiNoNa("Candados de caja", viaje!!.inspeccionGeneral.candados, { v, o -> viewModel.updateInspeccion("17", v, o) }, "viajes/$viajeId/checklist/inspeccion_17.jpg", { viewModel.updateFotoInspeccion("17", it) })
                CampoInspeccionSiNoNa("Bandas de seguridad para la caja", viaje!!.inspeccionGeneral.bandas, { v, o -> viewModel.updateInspeccion("18", v, o) }, "viajes/$viajeId/checklist/inspeccion_18.jpg", { viewModel.updateFotoInspeccion("18", it) })

                // ========== DOCUMENTACIÓN ==========
                SeccionTitulo("4. Documentación y Equipo")

                CampoSiNo(
                    label = "Copia de SÚA (Seguro Social)",
                    valor = viaje!!.documentacionEquipo.licenciaChofer.valor,
                    observacion = viaje!!.documentacionEquipo.licenciaChofer.observacion,
                    onUpdate = { v, o -> viewModel.updateDocumentacion("sua", v, o) },
                    storagePath = "viajes/$viajeId/checklist/documentacion_sua.jpg",
                    fotoURL = viaje!!.documentacionEquipo.licenciaChofer.fotoURL,
                    onFotoCapturada = { viewModel.updateFotoDocumentacion("sua", it) }
                )

                CampoSiNo(
                    label = "Póliza de seguro vigente",
                    valor = viaje!!.documentacionEquipo.tarjetaCirculacion.valor,
                    observacion = viaje!!.documentacionEquipo.tarjetaCirculacion.observacion,
                    onUpdate = { v, o -> viewModel.updateDocumentacion("poliza", v, o) },
                    storagePath = "viajes/$viajeId/checklist/documentacion_poliza.jpg",
                    fotoURL = viaje!!.documentacionEquipo.tarjetaCirculacion.fotoURL,
                    onFotoCapturada = { viewModel.updateFotoDocumentacion("poliza", it) }
                )

                CampoSiNo(
                    label = "Tarjeta de circulación",
                    valor = viaje!!.documentacionEquipo.segurosVehiculo.valor,
                    observacion = viaje!!.documentacionEquipo.segurosVehiculo.observacion,
                    onUpdate = { v, o -> viewModel.updateDocumentacion("tarjeta", v, o) },
                    storagePath = "viajes/$viajeId/checklist/documentacion_tarjeta.jpg",
                    fotoURL = viaje!!.documentacionEquipo.segurosVehiculo.fotoURL,
                    onFotoCapturada = { viewModel.updateFotoDocumentacion("tarjeta", it) }
                )

                CampoSiNo(
                    label = "Equipo de seguridad completo",
                    valor = viaje!!.documentacionEquipo.documentoViaje.valor,
                    observacion = viaje!!.documentacionEquipo.documentoViaje.observacion,
                    onUpdate = { v, o -> viewModel.updateDocumentacion("equipo", v, o) },
                    storagePath = "viajes/$viajeId/checklist/documentacion_equipo.jpg",
                    fotoURL = viaje!!.documentacionEquipo.documentoViaje.fotoURL,
                    onFotoCapturada = { viewModel.updateFotoDocumentacion("equipo", it) }
                )

                // ========== OBSERVACIONES ==========
                SeccionTitulo("5. Observaciones Generales")

                var observaciones by remember { mutableStateOf(viaje!!.observacionesGenerales) }
                OutlinedTextField(
                    value = observaciones,
                    onValueChange = {
                        observaciones = it
                        viewModel.updateObservacionesGenerales(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    minLines = 3,
                    label = { Text("Observaciones") }
                )

                // ========== BOTÓN GUARDAR ==========
                Button(
                    onClick = { viewModel.guardarChecklist() },
                    enabled = formularioCompleto && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Text("Guardar Checklist Completo")
                }

                if (!formularioCompleto) {
                    Text(
                        "Completa todos los campos requeridos",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SeccionTitulo(titulo: String) {
    Text(
        titulo,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
    )
}

@Composable
fun SeccionItinerario(
    destinosSeleccionados: List<DestinoCatalogo>,
    destinosCatalogo: List<DestinoCatalogo>,
    onAgregarDestino: (DestinoCatalogo) -> Unit,
    onEliminarDestino: (Int) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { expandido = !expandido },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ Agregar Destino")
        }

        if (expandido) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                destinosCatalogo.forEach { destino ->
                    OutlinedButton(
                        onClick = {
                            onAgregarDestino(destino)
                            expandido = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(destino.nombre)
                    }
                }
            }
        }

        destinosSeleccionados.forEachIndexed { index, destino ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${index + 1}. ${destino.nombre}")
                Button(onClick = { onEliminarDestino(index) }) {
                    Text("X")
                }
            }
        }
    }
}

@Composable
fun CampoSiNo(
    label: String,
    valor: String,
    observacion: String,
    onUpdate: (String, String) -> Unit,
    storagePath: String? = null,
    fotoURL: String? = null,
    onFotoCapturada: (String) -> Unit = {}
) {
    var obs by remember { mutableStateOf(observacion) }

    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onUpdate("SÍ", obs) },
                modifier = Modifier.weight(1f),
                colors = if (valor == "SÍ") ButtonDefaults.buttonColors()
                         else ButtonDefaults.outlinedButtonColors()
            ) {
                Text("SÍ")
            }
            Button(
                onClick = { onUpdate("NO", obs) },
                modifier = Modifier.weight(1f),
                colors = if (valor == "NO") ButtonDefaults.buttonColors()
                         else ButtonDefaults.outlinedButtonColors()
            ) {
                Text("NO")
            }
        }

        OutlinedTextField(
            value = obs,
            onValueChange = {
                obs = it
                if (valor.isNotEmpty()) {
                    onUpdate(valor, it)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Observación (opcional)") }
        )

        if (storagePath != null) {
            BotonFotoCamara(
                storagePath = storagePath,
                fotoURL = fotoURL,
                onFotoSubida = onFotoCapturada
            )
        }
    }
}

@Composable
fun CampoInspeccion(
    label: String,
    observacion: String,
    onUpdate: (String) -> Unit
) {
    var obs by remember { mutableStateOf(observacion) }

    Column(modifier = Modifier.padding(8.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = obs,
            onValueChange = {
                obs = it
                onUpdate(it)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Observación") }
        )
    }
}

@Composable
fun SeccionPresionLlantas(
    camion: com.example.checklistchofer.data.Camion?,
    presiones: List<PresionLlanta>,
    onUpdate: (List<PresionLlanta>) -> Unit
) {
    // Sin camión de catálogo (RENTA u OTRO) no hay un número fijo de llantas
    // real conocido, así que aquí sí se permite agregar/quitar filas.
    val esEditable = camion == null

    val llantasActuales = remember {
        val inicial = if (presiones.isNotEmpty()) {
            presiones
        } else {
            camion?.posicionesLlantas?.map { PresionLlanta(it, 0f) }
                ?: listOf(
                    PresionLlanta("Delantera Izquierda", 0f),
                    PresionLlanta("Delantera Derecha", 0f),
                    PresionLlanta("Trasera Izquierda Ext.", 0f),
                    PresionLlanta("Trasera Izquierda Int.", 0f),
                    PresionLlanta("Trasera Derecha Ext.", 0f),
                    PresionLlanta("Trasera Derecha Int.", 0f)
                )
        }
        androidx.compose.runtime.mutableStateListOf(*inicial.toTypedArray())
    }
    // Contador para nombrar llantas agregadas manualmente sin repetir etiqueta,
    // aunque se hayan quitado filas de en medio.
    var contadorLlantas by remember { mutableStateOf(llantasActuales.size) }

    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        llantasActuales.forEachIndexed { index, llanta ->
            // Se usa la etiqueta como key para que el texto no se desordene
            // al quitar una fila de en medio (los índices se recorren).
            var presion by remember(llanta.etiqueta) {
                mutableStateOf(if (llanta.presion != 0f) llanta.presion.toString() else "")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(llanta.etiqueta, modifier = Modifier.weight(1f))
                OutlinedTextField(
                    value = presion,
                    onValueChange = {
                        val filtered = it.filter { c -> c.isDigit() || c == '.' }
                        presion = filtered
                        llantasActuales[index] = llanta.copy(
                            presion = filtered.toFloatOrNull() ?: 0f
                        )
                        onUpdate(llantasActuales.toList())
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("PSI") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    )
                )
                if (esEditable) {
                    Button(
                        onClick = {
                            llantasActuales.removeAt(index)
                            onUpdate(llantasActuales.toList())
                        },
                        enabled = llantasActuales.size > 1
                    ) {
                        Text("X")
                    }
                }
            }
        }

        if (esEditable) {
            OutlinedButton(
                onClick = {
                    contadorLlantas += 1
                    llantasActuales.add(PresionLlanta("Llanta $contadorLlantas", 0f))
                    onUpdate(llantasActuales.toList())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Agregar llanta")
            }
        }
    }
}

@Composable
fun SeccionCombustibleThermo(
    valor: String,
    onUpdate: (String) -> Unit
) {
    val opciones = listOf("0", "1/4", "1/2", "3/4", "Lleno")
    var expandido by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(8.dp)) {
        OutlinedButton(
            onClick = { expandido = !expandido },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(valor.ifEmpty { "Seleccionar" })
        }

        if (expandido) {
            Column(modifier = Modifier.fillMaxWidth()) {
                opciones.forEach { opcion ->
                    OutlinedButton(
                        onClick = {
                            onUpdate(opcion)
                            expandido = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(opcion)
                    }
                }
            }
        }
    }
}

@Composable
fun SeccionObservacionFoto(
    observacion: String,
    fotoURL: String?,
    storagePath: String,
    onObservacionChange: (String) -> Unit,
    onFotoCapturada: (String) -> Unit
) {
    var obs by remember { mutableStateOf(observacion) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = obs,
            onValueChange = {
                obs = it
                onObservacionChange(it)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Observación (opcional)") }
        )
        BotonFotoCamara(
            storagePath = storagePath,
            fotoURL = fotoURL,
            onFotoSubida = onFotoCapturada
        )
    }
}

@Composable
fun CampoInspeccionSiNoNa(
    label: String,
    checkField: com.example.checklistchofer.data.CheckField,
    onUpdate: (String, String) -> Unit,
    storagePath: String? = null,
    onFotoCapturada: (String) -> Unit = {}
) {
    var obs by remember { mutableStateOf(checkField.observacion) }

    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { onUpdate("BIEN", obs) },
                modifier = Modifier.weight(1f),
                colors = if (checkField.valor == "BIEN") ButtonDefaults.buttonColors()
                         else ButtonDefaults.outlinedButtonColors()
            ) {
                Text("BIEN", fontSize = 11.sp)
            }
            Button(
                onClick = { onUpdate("MAL", obs) },
                modifier = Modifier.weight(1f),
                colors = if (checkField.valor == "MAL") ButtonDefaults.buttonColors()
                         else ButtonDefaults.outlinedButtonColors()
            ) {
                Text("MAL", fontSize = 11.sp)
            }
            Button(
                onClick = { onUpdate("N/A", obs) },
                modifier = Modifier.weight(1f),
                colors = if (checkField.valor == "N/A") ButtonDefaults.buttonColors()
                         else ButtonDefaults.outlinedButtonColors()
            ) {
                Text("N/A", fontSize = 11.sp)
            }
        }

        OutlinedTextField(
            value = obs,
            onValueChange = {
                obs = it
                if (checkField.valor.isNotEmpty()) {
                    onUpdate(checkField.valor, it)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Observación (opcional)") }
        )

        if (storagePath != null) {
            BotonFotoCamara(
                storagePath = storagePath,
                fotoURL = checkField.fotoURL,
                onFotoSubida = onFotoCapturada
            )
        }
    }
}
