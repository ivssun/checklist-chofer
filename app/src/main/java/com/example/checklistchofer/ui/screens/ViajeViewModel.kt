package com.example.checklistchofer.ui.screens

import android.content.SharedPreferences
import androidx.core.content.edit
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

// Borrador local (2026-09-01): si se cierra la app a medio llenar el
// formulario inicial (ANTES de que exista el doc del viaje en Firestore),
// se pierde todo. Se guarda cada campo en SharedPreferences y se restaura
// al reabrir, hasta que se crea el viaje (iniciarViaje) y se limpia.
private const val KEY_CHOFER_ID = "borrador_chofer_id"
private const val KEY_TIPO_UNIDAD = "borrador_tipo_unidad"
private const val KEY_PLACA_MANUAL = "borrador_placa_manual"
private const val KEY_USAR_PLACA_OTRO = "borrador_usar_placa_otro"
private const val KEY_DETALLE_RENTA = "borrador_detalle_renta"
private const val KEY_USAR_DETALLE_RENTA_OTRO = "borrador_usar_detalle_renta_otro"
private const val KEY_DETALLE_RENTA_MANUAL = "borrador_detalle_renta_manual"
private const val KEY_PLACA_RENTA = "borrador_placa_renta"
private const val KEY_CAMION_ID = "borrador_camion_id"
private const val KEY_ECONOMICO = "borrador_economico"

class ViajeViewModel(
    private val repository: FirebaseRepository = FirebaseRepository(),
    private val prefs: SharedPreferences? = null
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

                restaurarBorrador()

                _error.value = null
            } catch (e: Exception) {
                _error.value = "Error cargando catálogos: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========== BORRADOR LOCAL ==========

    private fun guardarBorrador() {
        val p = prefs ?: return
        p.edit {
            putString(KEY_CHOFER_ID, _choferSeleccionado.value?.id ?: "")
            putString(KEY_TIPO_UNIDAD, _tipoUnidad.value)
            putString(KEY_PLACA_MANUAL, _placaManual.value)
            putBoolean(KEY_USAR_PLACA_OTRO, _usarPlacaOtro.value)
            putString(KEY_DETALLE_RENTA, _detalleRenta.value)
            putBoolean(KEY_USAR_DETALLE_RENTA_OTRO, _usarDetalleRentaOtro.value)
            putString(KEY_DETALLE_RENTA_MANUAL, _detalleRentaManual.value)
            putString(KEY_PLACA_RENTA, _placaRentaSeleccionada.value)
            putString(KEY_CAMION_ID, _camionSeleccionado.value?.id ?: "")
            putString(KEY_ECONOMICO, _economicoManual.value)
        }
    }

    // No reutiliza cambiarTipoUnidad() porque esa función limpia las
    // selecciones dependientes (es para cuando el chofer CAMBIA el tipo a
    // mano, no para restaurar un borrador ya consistente).
    private fun restaurarBorrador() {
        val p = prefs ?: return

        val choferId = p.getString(KEY_CHOFER_ID, "") ?: ""
        if (choferId.isNotEmpty()) {
            _choferes.value.find { it.id == choferId }?.let { _choferSeleccionado.value = it }
        }

        val tipo = p.getString(KEY_TIPO_UNIDAD, "") ?: ""
        if (tipo.isNotEmpty()) {
            _tipoUnidad.value = tipo
            _camionesTopo.value = _camiones.value.filter { it.tipo == tipo }
        }

        _placaManual.value = p.getString(KEY_PLACA_MANUAL, "") ?: ""
        _usarPlacaOtro.value = p.getBoolean(KEY_USAR_PLACA_OTRO, false)
        _detalleRenta.value = p.getString(KEY_DETALLE_RENTA, "") ?: ""
        _usarDetalleRentaOtro.value = p.getBoolean(KEY_USAR_DETALLE_RENTA_OTRO, false)
        _detalleRentaManual.value = p.getString(KEY_DETALLE_RENTA_MANUAL, "") ?: ""
        _placaRentaSeleccionada.value = p.getString(KEY_PLACA_RENTA, "") ?: ""

        val camionId = p.getString(KEY_CAMION_ID, "") ?: ""
        if (camionId.isNotEmpty()) {
            _camionesTopo.value.find { it.id == camionId }?.let { _camionSeleccionado.value = it }
        }

        _economicoManual.value = p.getString(KEY_ECONOMICO, "") ?: ""
    }

    fun seleccionarChofer(chofer: Chofer) {
        _choferSeleccionado.value = chofer
        guardarBorrador()
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
        guardarBorrador()
    }

    fun seleccionarCamion(camion: Camion) {
        _camionSeleccionado.value = camion
        _usarPlacaOtro.value = false
        _placaManual.value = ""
        guardarBorrador()
    }

    fun seleccionarPlacaOtro() {
        _usarPlacaOtro.value = true
        _camionSeleccionado.value = null
        _placaRentaSeleccionada.value = ""
        guardarBorrador()
    }

    fun actualizarPlacaManual(placa: String) {
        _placaManual.value = placa
        guardarBorrador()
    }

    fun seleccionarDetalleRenta(marca: String) {
        _detalleRenta.value = marca
        _usarDetalleRentaOtro.value = false
        guardarBorrador()
    }

    fun seleccionarDetalleRentaOtro() {
        _usarDetalleRentaOtro.value = true
        _detalleRenta.value = ""
        guardarBorrador()
    }

    fun actualizarDetalleRentaManual(valor: String) {
        _detalleRentaManual.value = valor
        guardarBorrador()
    }

    fun seleccionarPlacaRenta(placa: String) {
        _placaRentaSeleccionada.value = placa
        _usarPlacaOtro.value = false
        guardarBorrador()
    }

    fun actualizarEconomicoManual(economico: String) {
        _economicoManual.value = economico
        guardarBorrador()
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

                // El viaje ya existe en Firestore; el borrador local ya no aplica
                prefs?.edit { clear() }

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
