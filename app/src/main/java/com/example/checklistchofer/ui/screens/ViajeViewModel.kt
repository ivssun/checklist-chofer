package com.example.checklistchofer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.checklistchofer.data.Camion
import com.example.checklistchofer.data.Chofer
import com.example.checklistchofer.data.FirebaseRepository
import com.example.checklistchofer.data.Viaje
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class ViajeViewModel(
    private val repository: FirebaseRepository = FirebaseRepository()
) : ViewModel() {

    // ========== STATE ==========

    private val _choferes = MutableStateFlow<List<Chofer>>(emptyList())
    val choferes: StateFlow<List<Chofer>> = _choferes.asStateFlow()

    private val _camiones = MutableStateFlow<List<Camion>>(emptyList())
    val camiones: StateFlow<List<Camion>> = _camiones.asStateFlow()

    private val _choferSeleccionado = MutableStateFlow<Chofer?>(null)
    val choferSeleccionado: StateFlow<Chofer?> = _choferSeleccionado.asStateFlow()

    private val _camionSeleccionado = MutableStateFlow<Camion?>(null)
    val camionSeleccionado: StateFlow<Camion?> = _camionSeleccionado.asStateFlow()

    private val _tipoUnidad = MutableStateFlow("")
    val tipoUnidad: StateFlow<String> = _tipoUnidad.asStateFlow()

    private val _placaManual = MutableStateFlow("")
    val placaManual: StateFlow<String> = _placaManual.asStateFlow()

    // "Otro" dentro del dropdown de placa (aplica a GDE/MED y RENTA; usa _placaManual como texto)
    private val _usarPlacaOtro = MutableStateFlow(false)
    val usarPlacaOtro: StateFlow<Boolean> = _usarPlacaOtro.asStateFlow()

    private val _camionesTopo = MutableStateFlow<List<Camion>>(emptyList())
    val camionesFiltered: StateFlow<List<Camion>> = _camionesTopo.asStateFlow()

    private val _marcasRenta = listOf("Ford", "Toyota", "Caja", "Redila", "Batea", "Volteo")
    private val _detalleRenta = MutableStateFlow("")
    val detalleRenta: StateFlow<String> = _detalleRenta.asStateFlow()

    private val _usarDetalleRentaOtro = MutableStateFlow(false)
    val usarDetalleRentaOtro: StateFlow<Boolean> = _usarDetalleRentaOtro.asStateFlow()

    private val _detalleRentaManual = MutableStateFlow("")
    val detalleRentaManual: StateFlow<String> = _detalleRentaManual.asStateFlow()

    private val _placarenta = listOf("RENTA001", "RENTA002", "RENTA003", "RENTA004", "RENTA005", "RENTA006")
    private val _placaRentaSeleccionada = MutableStateFlow("")
    val placaRentaSeleccionada: StateFlow<String> = _placaRentaSeleccionada.asStateFlow()

    private val _economicoManual = MutableStateFlow("")
    val economicoManual: StateFlow<String> = _economicoManual.asStateFlow()

    fun getMarcasRenta(): List<String> = _marcasRenta
    fun getPlacasRenta(): List<String> = _placarenta

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _viajeCreado = MutableStateFlow<String?>(null)
    val viajeCreado: StateFlow<String?> = _viajeCreado.asStateFlow()

    // ========== INIT ==========

    init {
        cargarCatalogos()
    }

    // ========== FUNCIONES PÚBLICAS ==========

    private fun cargarCatalogos() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Inicializar colecciones (solo crea si están vacías)
                repository.initializeCollections()

                // Cargar catálogos
                val choferesCargados = repository.getChoferes()
                val camionsCargados = repository.getCamiones()

                _choferes.value = choferesCargados
                _camiones.value = camionsCargados

                _error.value = null
            } catch (e: Exception) {
                _error.value = "Error cargando catálogos: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun seleccionarChofer(chofer: Chofer) {
        _choferSeleccionado.value = chofer
    }

    fun cambiarTipoUnidad(tipo: String) {
        _tipoUnidad.value = tipo
        // Filtrar camiones por tipo
        _camionesTopo.value = _camiones.value.filter { it.tipo == tipo }
        // Limpiar selección anterior
        _camionSeleccionado.value = null
        _placaManual.value = ""
        _usarPlacaOtro.value = false
        _detalleRenta.value = ""
        _usarDetalleRentaOtro.value = false
        _detalleRentaManual.value = ""
        _placaRentaSeleccionada.value = ""
        _economicoManual.value = ""
    }

    fun seleccionarCamion(camion: Camion) {
        _camionSeleccionado.value = camion
        _usarPlacaOtro.value = false
        _placaManual.value = ""
    }

    fun seleccionarPlacaOtro() {
        _usarPlacaOtro.value = true
        _camionSeleccionado.value = null
        _placaRentaSeleccionada.value = ""
    }

    fun actualizarPlacaManual(placa: String) {
        _placaManual.value = placa
    }

    fun seleccionarDetalleRenta(marca: String) {
        _detalleRenta.value = marca
        _usarDetalleRentaOtro.value = false
    }

    fun seleccionarDetalleRentaOtro() {
        _usarDetalleRentaOtro.value = true
        _detalleRenta.value = ""
    }

    fun actualizarDetalleRentaManual(valor: String) {
        _detalleRentaManual.value = valor
    }

    fun seleccionarPlacaRenta(placa: String) {
        _placaRentaSeleccionada.value = placa
        _usarPlacaOtro.value = false
    }

    fun actualizarEconomicoManual(economico: String) {
        _economicoManual.value = economico
    }

    fun iniciarViaje() {
        val chofer = _choferSeleccionado.value
        val tipo = _tipoUnidad.value
        val economico = _economicoManual.value.trim()

        // Validar selecciones básicas
        if (chofer == null) {
            _error.value = "Selecciona un chofer"
            return
        }

        if (economico.isEmpty()) {
            _error.value = "Ingresa el No. de Unidad / Económico"
            return
        }

        // Determinar camionId, placa y detalleRenta según tipo
        val camionId: String?
        val placa: String
        var detalleRentaVal = ""

        when (tipo) {
            "Otro" -> {
                val marcaVal = _detalleRentaManual.value.trim()
                if (marcaVal.isEmpty()) {
                    _error.value = "Ingresa la marca del vehículo"
                    return
                }
                val placaManualVal = _placaManual.value.trim()
                if (placaManualVal.isEmpty()) {
                    _error.value = "Ingresa la placa del vehículo"
                    return
                }
                camionId = null
                placa = placaManualVal
                detalleRentaVal = marcaVal
            }
            "RENTA" -> {
                if (_usarPlacaOtro.value) {
                    val placaManualVal = _placaManual.value.trim()
                    if (placaManualVal.isEmpty()) {
                        _error.value = "Ingresa la placa"
                        return
                    }
                    placa = placaManualVal
                } else {
                    val placaRenta = _placaRentaSeleccionada.value
                    if (placaRenta.isEmpty()) {
                        _error.value = "Selecciona una placa RENTA"
                        return
                    }
                    placa = placaRenta
                }

                if (_usarDetalleRentaOtro.value) {
                    val detalleManualVal = _detalleRentaManual.value.trim()
                    if (detalleManualVal.isEmpty()) {
                        _error.value = "Ingresa el detalle de marca"
                        return
                    }
                    detalleRentaVal = detalleManualVal
                } else {
                    if (_detalleRenta.value.isEmpty()) {
                        _error.value = "Selecciona un detalle de marca"
                        return
                    }
                    detalleRentaVal = _detalleRenta.value
                }

                camionId = null
            }
            else -> {
                if (_usarPlacaOtro.value) {
                    val placaManualVal = _placaManual.value.trim()
                    if (placaManualVal.isEmpty()) {
                        _error.value = "Ingresa la placa"
                        return
                    }
                    camionId = null
                    placa = placaManualVal
                } else {
                    val camion = _camionSeleccionado.value
                    if (camion == null) {
                        _error.value = "Selecciona un camión"
                        return
                    }
                    camionId = camion.id
                    placa = camion.placa
                }
            }
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Crear viaje nuevo con valores iniciales
                // horaLlegadaMatriz: se captura automáticamente aquí (chofer no puede
                // permanecer mucho tiempo en la matriz); se guarda pero no se muestra en UI.
                val nuevoViaje = Viaje(
                    choferId = chofer.id,
                    camionId = camionId,
                    tipoUnidad = tipo,
                    placa = placa,
                    detalleRenta = detalleRentaVal,
                    economico = economico,
                    fecha = Timestamp(Date()),
                    horaLlegadaMatriz = Timestamp(Date()),
                    concluido = false
                )

                // Guardar en Firestore
                val viajeId = repository.createViaje(nuevoViaje)

                // Notificar éxito
                _viajeCreado.value = viajeId

            } catch (e: Exception) {
                _error.value = "Error creando viaje: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun limpiarViajeCreado() {
        _viajeCreado.value = null
    }

    fun limpiarError() {
        _error.value = null
    }
}
