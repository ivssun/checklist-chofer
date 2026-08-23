package com.example.checklistchofer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.checklistchofer.data.Camion
import com.example.checklistchofer.data.CheckField
import com.example.checklistchofer.data.CombustibleYLimpieza
import com.example.checklistchofer.data.Destino
import com.example.checklistchofer.data.DestinoCatalogo
import com.example.checklistchofer.data.DocumentacionEquipo
import com.example.checklistchofer.data.FirebaseRepository
import com.example.checklistchofer.data.InspeccionGeneral
import com.example.checklistchofer.data.ObservacionFoto
import com.example.checklistchofer.data.PresionLlanta
import com.example.checklistchofer.data.Viaje
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChecklistViewModel(
    private val viajeId: String,
    private val repository: FirebaseRepository = FirebaseRepository()
) : ViewModel() {

    private val _viaje = MutableStateFlow<Viaje?>(null)
    val viaje: StateFlow<Viaje?> = _viaje.asStateFlow()

    private val _camion = MutableStateFlow<Camion?>(null)
    val camion: StateFlow<Camion?> = _camion.asStateFlow()

    private val _destinosCatalogo = MutableStateFlow<List<DestinoCatalogo>>(emptyList())
    val destinosCatalogo: StateFlow<List<DestinoCatalogo>> = _destinosCatalogo.asStateFlow()

    private val _destinosSeleccionados = MutableStateFlow<List<DestinoCatalogo>>(emptyList())
    val destinosSeleccionados: StateFlow<List<DestinoCatalogo>> = _destinosSeleccionados.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _checklistGuardado = MutableStateFlow(false)
    val checklistGuardado: StateFlow<Boolean> = _checklistGuardado.asStateFlow()

    private val _formularioCompleto = MutableStateFlow(false)
    val formularioCompleto: StateFlow<Boolean> = _formularioCompleto.asStateFlow()

    init {
        cargarDatos()
    }

    private fun cargarDatos() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val viajeActual = repository.getViajeById(viajeId)
                _viaje.value = viajeActual

                if (viajeActual != null) {
                    // Solo cargar camión si camionId no es null (no es RENTA ni Otro)
                    if (viajeActual.camionId != null) {
                        val camion = repository.getCamionById(viajeActual.camionId!!)
                        _camion.value = camion
                    }

                    val destinos = repository.getDestinosCatalogo()
                    _destinosCatalogo.value = destinos

                    val seleccionados = destinos.filter { it.id in viajeActual.destinosSeleccionados }
                    _destinosSeleccionados.value = seleccionados
                }

                _error.value = null
            } catch (e: Exception) {
                _error.value = "Error cargando viaje: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========== ITINERARIO ==========

    fun agregarDestino(destino: DestinoCatalogo) {
        val actuales = _destinosSeleccionados.value.toMutableList()
        actuales.add(destino)
        _destinosSeleccionados.value = actuales

        _viaje.value = _viaje.value?.copy(
            destinosSeleccionados = actuales.map { it.id }
        )
        validarFormulario()
    }

    fun eliminarDestino(index: Int) {
        val actuales = _destinosSeleccionados.value.toMutableList()
        if (index >= 0 && index < actuales.size) {
            actuales.removeAt(index)
            _destinosSeleccionados.value = actuales

            _viaje.value = _viaje.value?.copy(
                destinosSeleccionados = actuales.map { it.id }
            )
            validarFormulario()
        }
    }

    // ========== COMBUSTIBLE Y LIMPIEZA ==========

    fun updateTanqueLlenoSalida(valor: String, observacion: String) {
        _viaje.value = _viaje.value?.copy(
            combustibleYLimpieza = _viaje.value!!.combustibleYLimpieza.copy(
                tanqueLlenoSalida = _viaje.value!!.combustibleYLimpieza.tanqueLlenoSalida.copy(
                    valor = valor, observacion = observacion
                )
            )
        )
        validarFormulario()
    }

    fun updateTanqueLlenoRegreso(valor: String, observacion: String) {
        _viaje.value = _viaje.value?.copy(
            combustibleYLimpieza = _viaje.value!!.combustibleYLimpieza.copy(
                tanqueLlenoRegreso = _viaje.value!!.combustibleYLimpieza.tanqueLlenoRegreso.copy(
                    valor = valor, observacion = observacion
                )
            )
        )
        validarFormulario()
    }

    fun updateLimpiezaCajaCabina(valor: String, observacion: String) {
        _viaje.value = _viaje.value?.copy(
            combustibleYLimpieza = _viaje.value!!.combustibleYLimpieza.copy(
                limpiezaCajaCabina = _viaje.value!!.combustibleYLimpieza.limpiezaCajaCabina.copy(
                    valor = valor, observacion = observacion
                )
            )
        )
        validarFormulario()
    }

    fun updateFotoCombustible(campo: String, url: String) {
        val c = _viaje.value?.combustibleYLimpieza ?: return
        val nuevo = when (campo) {
            "salida" -> c.copy(tanqueLlenoSalida = c.tanqueLlenoSalida.copy(fotoURL = url))
            "regreso" -> c.copy(tanqueLlenoRegreso = c.tanqueLlenoRegreso.copy(fotoURL = url))
            "limpieza" -> c.copy(limpiezaCajaCabina = c.limpiezaCajaCabina.copy(fotoURL = url))
            else -> c
        }
        _viaje.value = _viaje.value?.copy(combustibleYLimpieza = nuevo)
    }

    // ========== INSPECCIÓN GENERAL (18 CAMPOS) ==========

    fun updateInspeccion(campo: String, valor: String, observacion: String) {
        val inspeccion = _viaje.value?.inspeccionGeneral ?: InspeccionGeneral()

        val nuevaInspeccion = when (campo) {
            "1" -> inspeccion.copy(llantasDesgaste = inspeccion.llantasDesgaste.copy(valor = valor, observacion = observacion))
            "2" -> inspeccion.copy(llantaRefaccion = inspeccion.llantaRefaccion.copy(valor = valor, observacion = observacion))
            "4" -> inspeccion.copy(sistemaFrenado = inspeccion.sistemaFrenado.copy(valor = valor, observacion = observacion))
            "5" -> inspeccion.copy(luces = inspeccion.luces.copy(valor = valor, observacion = observacion))
            "6" -> inspeccion.copy(espejos = inspeccion.espejos.copy(valor = valor, observacion = observacion))
            "7" -> inspeccion.copy(limpiaparabrisas = inspeccion.limpiaparabrisas.copy(valor = valor, observacion = observacion))
            "8" -> inspeccion.copy(nivelAceite = inspeccion.nivelAceite.copy(valor = valor, observacion = observacion))
            "9" -> inspeccion.copy(nivelAgua = inspeccion.nivelAgua.copy(valor = valor, observacion = observacion))
            "10" -> inspeccion.copy(nivelLiquidoFreno = inspeccion.nivelLiquidoFreno.copy(valor = valor, observacion = observacion))
            "13" -> inspeccion.copy(bateria = inspeccion.bateria.copy(valor = valor, observacion = observacion))
            "14" -> inspeccion.copy(triangulos = inspeccion.triangulos.copy(valor = valor, observacion = observacion))
            "15" -> inspeccion.copy(gato = inspeccion.gato.copy(valor = valor, observacion = observacion))
            "16" -> inspeccion.copy(carroceria = inspeccion.carroceria.copy(valor = valor, observacion = observacion))
            "17" -> inspeccion.copy(candados = inspeccion.candados.copy(valor = valor, observacion = observacion))
            "18" -> inspeccion.copy(bandas = inspeccion.bandas.copy(valor = valor, observacion = observacion))
            else -> inspeccion
        }

        _viaje.value = _viaje.value?.copy(inspeccionGeneral = nuevaInspeccion)
        validarFormulario()
    }

    fun updateFotoInspeccion(campo: String, url: String) {
        val inspeccion = _viaje.value?.inspeccionGeneral ?: return

        val nuevaInspeccion = when (campo) {
            "1" -> inspeccion.copy(llantasDesgaste = inspeccion.llantasDesgaste.copy(fotoURL = url))
            "2" -> inspeccion.copy(llantaRefaccion = inspeccion.llantaRefaccion.copy(fotoURL = url))
            "4" -> inspeccion.copy(sistemaFrenado = inspeccion.sistemaFrenado.copy(fotoURL = url))
            "5" -> inspeccion.copy(luces = inspeccion.luces.copy(fotoURL = url))
            "6" -> inspeccion.copy(espejos = inspeccion.espejos.copy(fotoURL = url))
            "7" -> inspeccion.copy(limpiaparabrisas = inspeccion.limpiaparabrisas.copy(fotoURL = url))
            "8" -> inspeccion.copy(nivelAceite = inspeccion.nivelAceite.copy(fotoURL = url))
            "9" -> inspeccion.copy(nivelAgua = inspeccion.nivelAgua.copy(fotoURL = url))
            "10" -> inspeccion.copy(nivelLiquidoFreno = inspeccion.nivelLiquidoFreno.copy(fotoURL = url))
            "13" -> inspeccion.copy(bateria = inspeccion.bateria.copy(fotoURL = url))
            "14" -> inspeccion.copy(triangulos = inspeccion.triangulos.copy(fotoURL = url))
            "15" -> inspeccion.copy(gato = inspeccion.gato.copy(fotoURL = url))
            "16" -> inspeccion.copy(carroceria = inspeccion.carroceria.copy(fotoURL = url))
            "17" -> inspeccion.copy(candados = inspeccion.candados.copy(fotoURL = url))
            "18" -> inspeccion.copy(bandas = inspeccion.bandas.copy(fotoURL = url))
            else -> inspeccion
        }

        _viaje.value = _viaje.value?.copy(inspeccionGeneral = nuevaInspeccion)
    }

    fun updatePresionLlantas(llantas: List<PresionLlanta>) {
        _viaje.value = _viaje.value?.copy(presionLlantas = llantas)
        validarFormulario()
    }

    fun updatePresionLlantasObservacion(observacion: String) {
        val actual = _viaje.value?.presionLlantasObservacion ?: ObservacionFoto()
        _viaje.value = _viaje.value?.copy(presionLlantasObservacion = actual.copy(observacion = observacion))
    }

    fun updateFotoPresionLlantas(url: String) {
        val actual = _viaje.value?.presionLlantasObservacion ?: ObservacionFoto()
        _viaje.value = _viaje.value?.copy(presionLlantasObservacion = actual.copy(fotoURL = url))
    }

    fun updateUreaPorcentaje(valor: Int) {
        _viaje.value = _viaje.value?.copy(ureaPorcentaje = valor)
        validarFormulario()
    }

    fun updateUreaObservacion(observacion: String) {
        val actual = _viaje.value?.ureaObservacion ?: ObservacionFoto()
        _viaje.value = _viaje.value?.copy(ureaObservacion = actual.copy(observacion = observacion))
    }

    fun updateFotoUrea(url: String) {
        val actual = _viaje.value?.ureaObservacion ?: ObservacionFoto()
        _viaje.value = _viaje.value?.copy(ureaObservacion = actual.copy(fotoURL = url))
    }

    fun updateCombustibleThermo(valor: String) {
        _viaje.value = _viaje.value?.copy(combustibleThermo = valor)
        validarFormulario()
    }

    fun updateCombustibleThermoObservacion(observacion: String) {
        val actual = _viaje.value?.combustibleThermoObservacion ?: ObservacionFoto()
        _viaje.value = _viaje.value?.copy(combustibleThermoObservacion = actual.copy(observacion = observacion))
    }

    fun updateFotoCombustibleThermo(url: String) {
        val actual = _viaje.value?.combustibleThermoObservacion ?: ObservacionFoto()
        _viaje.value = _viaje.value?.copy(combustibleThermoObservacion = actual.copy(fotoURL = url))
    }

    // ========== DOCUMENTACIÓN ==========

    fun updateDocumentacion(campo: String, valor: String, observacion: String) {
        val doc = _viaje.value?.documentacionEquipo ?: DocumentacionEquipo()

        val nueva = when (campo) {
            "sua" -> doc.copy(licenciaChofer = doc.licenciaChofer.copy(valor = valor, observacion = observacion))
            "poliza" -> doc.copy(tarjetaCirculacion = doc.tarjetaCirculacion.copy(valor = valor, observacion = observacion))
            "tarjeta" -> doc.copy(segurosVehiculo = doc.segurosVehiculo.copy(valor = valor, observacion = observacion))
            "equipo" -> doc.copy(documentoViaje = doc.documentoViaje.copy(valor = valor, observacion = observacion))
            else -> doc
        }

        _viaje.value = _viaje.value?.copy(documentacionEquipo = nueva)
        validarFormulario()
    }

    fun updateFotoDocumentacion(campo: String, url: String) {
        val doc = _viaje.value?.documentacionEquipo ?: return

        val nueva = when (campo) {
            "sua" -> doc.copy(licenciaChofer = doc.licenciaChofer.copy(fotoURL = url))
            "poliza" -> doc.copy(tarjetaCirculacion = doc.tarjetaCirculacion.copy(fotoURL = url))
            "tarjeta" -> doc.copy(segurosVehiculo = doc.segurosVehiculo.copy(fotoURL = url))
            "equipo" -> doc.copy(documentoViaje = doc.documentoViaje.copy(fotoURL = url))
            else -> doc
        }

        _viaje.value = _viaje.value?.copy(documentacionEquipo = nueva)
    }

    // ========== OBSERVACIONES ==========

    fun updateObservacionesGenerales(observaciones: String) {
        _viaje.value = _viaje.value?.copy(observacionesGenerales = observaciones)
    }

    // ========== VALIDACIÓN ==========

    private fun validarFormulario() {
        val v = _viaje.value ?: return

        val tieneDestinos = v.destinosSeleccionados.isNotEmpty()

        val combustibleCompleto = v.combustibleYLimpieza.tanqueLlenoSalida.valor.isNotEmpty() &&
                v.combustibleYLimpieza.tanqueLlenoRegreso.valor.isNotEmpty() &&
                v.combustibleYLimpieza.limpiezaCajaCabina.valor.isNotEmpty()

        val inspeccionCompleta = v.inspeccionGeneral.llantasDesgaste.valor.isNotEmpty() &&
                v.inspeccionGeneral.llantaRefaccion.valor.isNotEmpty() &&
                v.inspeccionGeneral.sistemaFrenado.valor.isNotEmpty() &&
                v.inspeccionGeneral.luces.valor.isNotEmpty() &&
                v.inspeccionGeneral.espejos.valor.isNotEmpty() &&
                v.inspeccionGeneral.limpiaparabrisas.valor.isNotEmpty() &&
                v.inspeccionGeneral.nivelAceite.valor.isNotEmpty() &&
                v.inspeccionGeneral.nivelAgua.valor.isNotEmpty() &&
                v.inspeccionGeneral.nivelLiquidoFreno.valor.isNotEmpty() &&
                v.inspeccionGeneral.bateria.valor.isNotEmpty() &&
                v.inspeccionGeneral.triangulos.valor.isNotEmpty() &&
                v.inspeccionGeneral.gato.valor.isNotEmpty() &&
                v.inspeccionGeneral.carroceria.valor.isNotEmpty() &&
                v.inspeccionGeneral.candados.valor.isNotEmpty() &&
                v.inspeccionGeneral.bandas.valor.isNotEmpty() &&
                v.presionLlantas.isNotEmpty() &&
                v.presionLlantas.all { it.presion > 0 } &&
                v.combustibleThermo.isNotEmpty()

        val documentacionCompleta = v.documentacionEquipo.licenciaChofer.valor.isNotEmpty() &&
                v.documentacionEquipo.tarjetaCirculacion.valor.isNotEmpty() &&
                v.documentacionEquipo.segurosVehiculo.valor.isNotEmpty() &&
                v.documentacionEquipo.documentoViaje.valor.isNotEmpty()

        _formularioCompleto.value = tieneDestinos && combustibleCompleto && inspeccionCompleta && documentacionCompleta
    }

    // ========== GUARDAR ==========

    fun guardarChecklist() {
        val viajeActual = _viaje.value ?: return

        if (!_formularioCompleto.value) {
            _error.value = "Completa todos los campos requeridos"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.updateViaje(viajeActual)

                _destinosSeleccionados.value.forEachIndexed { index, destinoCatalogo ->
                    repository.addDestino(
                        viajeId,
                        Destino(orden = index, cedisDestino = destinoCatalogo.nombre)
                    )
                }

                _checklistGuardado.value = true
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Error guardando checklist: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun limpiarError() {
        _error.value = null
    }
}
