package com.example.checklistchofer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.checklistchofer.data.DestinoCatalogo
import com.example.checklistchofer.data.PresionLlanta
import com.example.checklistchofer.data.Viaje

private enum class SeccionChecklist(val titulo: String) {
    ITINERARIO("1. Itinerario del Viaje"),
    COMBUSTIBLE("2. Combustible y Limpieza"),
    INSPECCION("3. Inspección General"),
    DOCUMENTACION("4. Documentación y Equipo"),
    OBSERVACIONES("5. Observaciones Generales")
}

private fun contarCombustible(v: Viaje): Pair<Int, Int> {
    val campos = listOf(
        v.combustibleYLimpieza.tanqueLlenoSalida.valor,
        v.combustibleYLimpieza.tanqueLlenoRegreso.valor,
        v.combustibleYLimpieza.limpiezaCajaCabina.valor
    )
    return campos.count { it.isNotEmpty() } to campos.size
}

private fun contarInspeccion(v: Viaje): Pair<Int, Int> {
    val i = v.inspeccionGeneral
    val campos = listOf(
        i.llantasDesgaste.valor, i.llantaRefaccion.valor, i.sistemaFrenado.valor, i.luces.valor,
        i.espejos.valor, i.limpiaparabrisas.valor, i.nivelAceite.valor, i.nivelAgua.valor,
        i.nivelLiquidoFreno.valor, i.bateria.valor, i.triangulos.valor, i.gato.valor,
        i.carroceria.valor, i.candados.valor, i.bandas.valor
    )
    var completados = campos.count { it.isNotEmpty() }
    if (v.presionLlantas.isNotEmpty() && v.presionLlantas.all { it.presion > 0 }) completados++
    if (v.combustibleThermo.isNotEmpty()) completados++
    val total = campos.size + 2 // + presión de llantas + combustible thermo
    return completados to total
}

private fun contarDocumentacion(v: Viaje): Pair<Int, Int> {
    val d = v.documentacionEquipo
    val campos = listOf(
        d.licenciaChofer.valor, d.tarjetaCirculacion.valor, d.segurosVehiculo.valor, d.documentoViaje.valor
    )
    return campos.count { it.isNotEmpty() } to campos.size
}

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

    var seccionAbierta by remember { mutableStateOf<SeccionChecklist?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = seccionAbierta != null) { seccionAbierta = null }

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
                title = { Text(seccionAbierta?.titulo ?: "Checklist Viaje") },
                navigationIcon = {
                    if (seccionAbierta != null) {
                        IconButton(onClick = { seccionAbierta = null }) {
                            Text("←", fontSize = 22.sp, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
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
            val v = viaje!!

            if (seccionAbierta == null) {
                ChecklistHub(
                    viaje = v,
                    destinosSeleccionados = destinosSeleccionados,
                    formularioCompleto = formularioCompleto,
                    isLoading = isLoading,
                    onAbrirSeccion = { seccionAbierta = it },
                    onGuardar = { viewModel.guardarChecklist() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (seccionAbierta) {
                        SeccionChecklist.ITINERARIO -> SeccionItinerario(
                            destinosSeleccionados = destinosSeleccionados,
                            destinosCatalogo = destinosCatalogo,
                            onAgregarDestino = { viewModel.agregarDestino(it) },
                            onEliminarDestino = { viewModel.eliminarDestino(it) }
                        )

                        SeccionChecklist.COMBUSTIBLE -> {
                            CampoSiNo(
                                label = "¿Salió con tanque lleno diésel?",
                                valor = v.combustibleYLimpieza.tanqueLlenoSalida.valor,
                                observacion = v.combustibleYLimpieza.tanqueLlenoSalida.observacion,
                                onUpdate = { valor, obs -> viewModel.updateTanqueLlenoSalida(valor, obs) },
                                storagePath = "viajes/$viajeId/checklist/combustible_salida.jpg",
                                fotoURL = v.combustibleYLimpieza.tanqueLlenoSalida.fotoURL,
                                onFotoCapturada = { viewModel.updateFotoCombustible("salida", it) }
                            )

                            CampoSiNo(
                                label = "¿Regresó con tanque lleno?",
                                valor = v.combustibleYLimpieza.tanqueLlenoRegreso.valor,
                                observacion = v.combustibleYLimpieza.tanqueLlenoRegreso.observacion,
                                onUpdate = { valor, obs -> viewModel.updateTanqueLlenoRegreso(valor, obs) },
                                storagePath = "viajes/$viajeId/checklist/combustible_regreso.jpg",
                                fotoURL = v.combustibleYLimpieza.tanqueLlenoRegreso.fotoURL,
                                onFotoCapturada = { viewModel.updateFotoCombustible("regreso", it) }
                            )

                            CampoSiNo(
                                label = "¿Se limpió caja y cabina?",
                                valor = v.combustibleYLimpieza.limpiezaCajaCabina.valor,
                                observacion = v.combustibleYLimpieza.limpiezaCajaCabina.observacion,
                                onUpdate = { valor, obs -> viewModel.updateLimpiezaCajaCabina(valor, obs) },
                                storagePath = "viajes/$viajeId/checklist/combustible_limpieza.jpg",
                                fotoURL = v.combustibleYLimpieza.limpiezaCajaCabina.fotoURL,
                                onFotoCapturada = { viewModel.updateFotoCombustible("limpieza", it) }
                            )
                        }

                        SeccionChecklist.INSPECCION -> {
                            CampoInspeccionSiNoNa("Llantas (desgaste)", v.inspeccionGeneral.llantasDesgaste, { valor, obs -> viewModel.updateInspeccion("1", valor, obs) }, "viajes/$viajeId/checklist/inspeccion_1.jpg", { viewModel.updateFotoInspeccion("1", it) })
                            CampoInspeccionSiNoNa("Llanta de refacción", v.inspeccionGeneral.llantaRefaccion, { valor, obs -> viewModel.updateInspeccion("2", valor, obs) }, "viajes/$viajeId/checklist/inspeccion_2.jpg", { viewModel.updateFotoInspeccion("2", it) })

                            Text("Presión de llantas (poner numero)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            SeccionPresionLlantas(
                                camion = camion,
                                presiones = v.presionLlantas,
                                onUpdate = { viewModel.updatePresionLlantas(it) }
                            )
                            SeccionObservacionFoto(
                                observacion = v.presionLlantasObservacion.observacion,
                                fotoURL = v.presionLlantasObservacion.fotoURL,
                                storagePath = "viajes/$viajeId/checklist/presion_llantas.jpg",
                                onObservacionChange = { viewModel.updatePresionLlantasObservacion(it) },
                                onFotoCapturada = { viewModel.updateFotoPresionLlantas(it) }
                            )

                            CampoInspeccionSiNoNa("Sistema de frenado óptimo", v.inspeccionGeneral.sistemaFrenado, { valor, obs -> viewModel.updateInspeccion("4", valor, obs) }, "viajes/$viajeId/checklist/inspeccion_4.jpg", { viewModel.updateFotoInspeccion("4", it) })
                            CampoInspeccionSiNoNa("Luces (altas, bajas, direccionales, reversa, stop)", v.inspeccionGeneral.luces, { valor, obs -> viewModel.updateInspeccion("5", valor, obs) }, "viajes/$viajeId/checklist/inspeccion_5.jpg", { viewModel.updateFotoInspeccion("5", it) })
                            CampoInspeccionSiNoNa("Espejos laterales y retrovisor", v.inspeccionGeneral.espejos, { valor, obs -> viewModel.updateInspeccion("6", valor, obs) }, "viajes/$viajeId/checklist/inspeccion_6.jpg", { viewModel.updateFotoInspeccion("6", it) })
                            CampoInspeccionSiNoNa("Limpiaparabrisas y claxon", v.inspeccionGeneral.limpiaparabrisas, { valor, obs -> viewModel.updateInspeccion("7", valor, obs) }, "viajes/$viajeId/checklist/inspeccion_7.jpg", { viewModel.updateFotoInspeccion("7", it) })
                            CampoInspeccionSiNoNa("Nivel de aceite de motor", v.inspeccionGeneral.nivelAceite, { valor, obs -> viewModel.updateInspeccion("8", valor, obs) }, "viajes/$viajeId/checklist/inspeccion_8.jpg", { viewModel.updateFotoInspeccion("8", it) })
                            CampoInspeccionSiNoNa("Nivel de agua / refrigerante", v.inspeccionGeneral.nivelAgua, { valor, obs -> viewModel.updateInspeccion("9", valor, obs) }, "viajes/$viajeId/checklist/inspeccion_9.jpg", { viewModel.updateFotoInspeccion("9", it) })
                            CampoInspeccionSiNoNa("Nivel de líquido de frenos (en caso de que aplique)", v.inspeccionGeneral.nivelLiquidoFreno, { valor, obs -> viewModel.updateInspeccion("10", valor, obs) }, "viajes/$viajeId/checklist/inspeccion_10.jpg", { viewModel.updateFotoInspeccion("10", it) })

                            Text("Nivel de Urea", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            var ureaValor by remember { mutableStateOf(if (v.ureaPorcentaje != 0) v.ureaPorcentaje.toString() else "") }
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
                                observacion = v.ureaObservacion.observacion,
                                fotoURL = v.ureaObservacion.fotoURL,
                                storagePath = "viajes/$viajeId/checklist/urea.jpg",
                                onObservacionChange = { viewModel.updateUreaObservacion(it) },
                                onFotoCapturada = { viewModel.updateFotoUrea(it) }
                            )

                            Text("Nivel de combustible para thermo", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            SeccionCombustibleThermo(
                                valor = v.combustibleThermo,
                                onUpdate = { viewModel.updateCombustibleThermo(it) }
                            )
                            SeccionObservacionFoto(
                                observacion = v.combustibleThermoObservacion.observacion,
                                fotoURL = v.combustibleThermoObservacion.fotoURL,
                                storagePath = "viajes/$viajeId/checklist/combustible_thermo.jpg",
                                onObservacionChange = { viewModel.updateCombustibleThermoObservacion(it) },
                                onFotoCapturada = { viewModel.updateFotoCombustibleThermo(it) }
                            )

                            CampoInspeccionSiNoNa("Batería (terminales y carga)", v.inspeccionGeneral.bateria, { valor, obs -> viewModel.updateInspeccion("13", valor, obs) }, "viajes/$viajeId/checklist/inspeccion_13.jpg", { viewModel.updateFotoInspeccion("13", it) })
                            CampoInspeccionSiNoNa("Triángulos de seguridad / señalización", v.inspeccionGeneral.triangulos, { valor, obs -> viewModel.updateInspeccion("14", valor, obs) }, "viajes/$viajeId/checklist/inspeccion_14.jpg", { viewModel.updateFotoInspeccion("14", it) })
                            CampoInspeccionSiNoNa("Gato hidráulico, cruceta y herramienta básica", v.inspeccionGeneral.gato, { valor, obs -> viewModel.updateInspeccion("15", valor, obs) }, "viajes/$viajeId/checklist/inspeccion_15.jpg", { viewModel.updateFotoInspeccion("15", it) })
                            CampoInspeccionSiNoNa("Estado de Carrocería (golpes, rayones, abolladuras)", v.inspeccionGeneral.carroceria, { valor, obs -> viewModel.updateInspeccion("16", valor, obs) }, "viajes/$viajeId/checklist/inspeccion_16.jpg", { viewModel.updateFotoInspeccion("16", it) })
                            CampoInspeccionSiNoNa("Candados de caja", v.inspeccionGeneral.candados, { valor, obs -> viewModel.updateInspeccion("17", valor, obs) }, "viajes/$viajeId/checklist/inspeccion_17.jpg", { viewModel.updateFotoInspeccion("17", it) })
                            CampoInspeccionSiNoNa("Bandas de seguridad para la caja", v.inspeccionGeneral.bandas, { valor, obs -> viewModel.updateInspeccion("18", valor, obs) }, "viajes/$viajeId/checklist/inspeccion_18.jpg", { viewModel.updateFotoInspeccion("18", it) })
                        }

                        SeccionChecklist.DOCUMENTACION -> {
                            CampoSiNo(
                                label = "Copia de SÚA (Seguro Social)",
                                valor = v.documentacionEquipo.licenciaChofer.valor,
                                observacion = v.documentacionEquipo.licenciaChofer.observacion,
                                onUpdate = { valor, obs -> viewModel.updateDocumentacion("sua", valor, obs) },
                                storagePath = "viajes/$viajeId/checklist/documentacion_sua.jpg",
                                fotoURL = v.documentacionEquipo.licenciaChofer.fotoURL,
                                onFotoCapturada = { viewModel.updateFotoDocumentacion("sua", it) }
                            )

                            CampoSiNo(
                                label = "Póliza de seguro vigente",
                                valor = v.documentacionEquipo.tarjetaCirculacion.valor,
                                observacion = v.documentacionEquipo.tarjetaCirculacion.observacion,
                                onUpdate = { valor, obs -> viewModel.updateDocumentacion("poliza", valor, obs) },
                                storagePath = "viajes/$viajeId/checklist/documentacion_poliza.jpg",
                                fotoURL = v.documentacionEquipo.tarjetaCirculacion.fotoURL,
                                onFotoCapturada = { viewModel.updateFotoDocumentacion("poliza", it) }
                            )

                            CampoSiNo(
                                label = "Tarjeta de circulación",
                                valor = v.documentacionEquipo.segurosVehiculo.valor,
                                observacion = v.documentacionEquipo.segurosVehiculo.observacion,
                                onUpdate = { valor, obs -> viewModel.updateDocumentacion("tarjeta", valor, obs) },
                                storagePath = "viajes/$viajeId/checklist/documentacion_tarjeta.jpg",
                                fotoURL = v.documentacionEquipo.segurosVehiculo.fotoURL,
                                onFotoCapturada = { viewModel.updateFotoDocumentacion("tarjeta", it) }
                            )

                            CampoSiNo(
                                label = "Equipo de seguridad completo",
                                valor = v.documentacionEquipo.documentoViaje.valor,
                                observacion = v.documentacionEquipo.documentoViaje.observacion,
                                onUpdate = { valor, obs -> viewModel.updateDocumentacion("equipo", valor, obs) },
                                storagePath = "viajes/$viajeId/checklist/documentacion_equipo.jpg",
                                fotoURL = v.documentacionEquipo.documentoViaje.fotoURL,
                                onFotoCapturada = { viewModel.updateFotoDocumentacion("equipo", it) }
                            )
                        }

                        SeccionChecklist.OBSERVACIONES -> {
                            var observaciones by remember { mutableStateOf(v.observacionesGenerales) }
                            OutlinedTextField(
                                value = observaciones,
                                onValueChange = {
                                    observaciones = it
                                    viewModel.updateObservacionesGenerales(it)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                label = { Text("Observaciones") },
                                placeholder = { Text("Reporta aquí cualquier cosa extraordinaria no cubierta por el formulario") }
                            )
                            BotonFotoCamara(
                                storagePath = "viajes/$viajeId/checklist/observaciones_generales.jpg",
                                fotoURL = v.observacionesGeneralesFotoURL,
                                onFotoSubida = { viewModel.updateFotoObservacionesGenerales(it) }
                            )
                        }

                        null -> Unit
                    }

                    Button(
                        onClick = { seccionAbierta = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Continuar llenando checklist")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistHub(
    viaje: Viaje,
    destinosSeleccionados: List<DestinoCatalogo>,
    formularioCompleto: Boolean,
    isLoading: Boolean,
    onAbrirSeccion: (SeccionChecklist) -> Unit,
    onGuardar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (combustibleOk, combustibleTotal) = contarCombustible(viaje)
    val (inspeccionOk, inspeccionTotal) = contarInspeccion(viaje)
    val (documentacionOk, documentacionTotal) = contarDocumentacion(viaje)
    val tieneObservaciones = viaje.observacionesGenerales.isNotBlank() || viaje.observacionesGeneralesFotoURL != null

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TarjetaSeccion(
            titulo = SeccionChecklist.ITINERARIO.titulo,
            subtitulo = if (destinosSeleccionados.isNotEmpty())
                "${destinosSeleccionados.size} destino(s) agregado(s)"
            else "Agrega al menos 1 destino",
            completados = destinosSeleccionados.size,
            total = if (destinosSeleccionados.isNotEmpty()) destinosSeleccionados.size else 1,
            completo = destinosSeleccionados.isNotEmpty(),
            onClick = { onAbrirSeccion(SeccionChecklist.ITINERARIO) }
        )
        TarjetaSeccion(
            titulo = SeccionChecklist.COMBUSTIBLE.titulo,
            subtitulo = "$combustibleOk de $combustibleTotal puntos",
            completados = combustibleOk,
            total = combustibleTotal,
            completo = combustibleOk == combustibleTotal,
            onClick = { onAbrirSeccion(SeccionChecklist.COMBUSTIBLE) }
        )
        TarjetaSeccion(
            titulo = SeccionChecklist.INSPECCION.titulo,
            subtitulo = "$inspeccionOk de $inspeccionTotal puntos",
            completados = inspeccionOk,
            total = inspeccionTotal,
            completo = inspeccionOk == inspeccionTotal,
            onClick = { onAbrirSeccion(SeccionChecklist.INSPECCION) }
        )
        TarjetaSeccion(
            titulo = SeccionChecklist.DOCUMENTACION.titulo,
            subtitulo = "$documentacionOk de $documentacionTotal puntos",
            completados = documentacionOk,
            total = documentacionTotal,
            completo = documentacionOk == documentacionTotal,
            onClick = { onAbrirSeccion(SeccionChecklist.DOCUMENTACION) }
        )
        TarjetaSeccion(
            titulo = SeccionChecklist.OBSERVACIONES.titulo,
            subtitulo = if (tieneObservaciones) "Con información" else "Opcional",
            completados = if (tieneObservaciones) 1 else 0,
            total = 1,
            completo = tieneObservaciones,
            onClick = { onAbrirSeccion(SeccionChecklist.OBSERVACIONES) }
        )

        Spacer(modifier = Modifier.padding(top = 8.dp))

        Button(
            onClick = onGuardar,
            enabled = formularioCompleto && !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar Checklist Completo")
        }

        if (!formularioCompleto) {
            Text(
                "Completa todos los campos requeridos",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun TarjetaSeccion(
    titulo: String,
    subtitulo: String,
    completados: Int,
    total: Int,
    completo: Boolean,
    onClick: () -> Unit
) {
    val fraccion = if (total > 0) completados / total.toFloat() else 0f
    val colorProgreso = if (completo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
                CircularProgressIndicator(
                    progress = { fraccion },
                    modifier = Modifier.size(44.dp),
                    color = colorProgreso,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 4.dp
                )
                Text(
                    if (completo) "✓" else "$completados",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorProgreso
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    subtitulo,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                "›",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    // Un FocusRequester por fila (recalculado cuando cambia el conjunto de
    // etiquetas) para poder saltar de una llanta a la siguiente con NEXT y
    // cerrar el teclado con DONE en la última.
    val focusRequesters = remember(llantasActuales.map { it.etiqueta }) {
        List(llantasActuales.size) { FocusRequester() }
    }

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
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequesters[index]),
                    singleLine = true,
                    placeholder = { Text("PSI") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = if (index == llantasActuales.lastIndex) ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusRequesters.getOrNull(index + 1)?.requestFocus() },
                        onDone = { focusManager.clearFocus() }
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
    // Opciones visibles directamente (sin dropdown) para ahorrarle al chofer
    // el click extra de desplegar antes de poder elegir.
    val filaSuperior = listOf("0", "1/4", "1/2")
    val filaInferior = listOf("3/4", "Lleno")

    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filaSuperior.forEach { opcion ->
                Button(
                    onClick = { onUpdate(opcion) },
                    modifier = Modifier.weight(1f),
                    colors = if (valor == opcion) ButtonDefaults.buttonColors()
                             else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text(opcion)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filaInferior.forEach { opcion ->
                Button(
                    onClick = { onUpdate(opcion) },
                    modifier = Modifier.weight(1f),
                    colors = if (valor == opcion) ButtonDefaults.buttonColors()
                             else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text(opcion)
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
