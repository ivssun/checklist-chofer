package com.example.checklistchofer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.checklistchofer.data.CargaCombustible
import com.example.checklistchofer.data.Camion
import com.example.checklistchofer.data.Destino
import com.example.checklistchofer.data.DestinoCatalogo
import com.example.checklistchofer.data.FirebaseRepository
import com.example.checklistchofer.data.Incidente
import com.example.checklistchofer.data.Viaje
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class ControlViajeViewModel(
    private val viajeId: String,
    private val repository: FirebaseRepository = FirebaseRepository()
) : ViewModel() {

    private val _viaje = MutableStateFlow<Viaje?>(null)
    val viaje: StateFlow<Viaje?> = _viaje.asStateFlow()

    private val _destinos = MutableStateFlow<List<Destino>>(emptyList())
    val destinos: StateFlow<List<Destino>> = _destinos.asStateFlow()

    private val _destinosCatalogo = MutableStateFlow<List<DestinoCatalogo>>(emptyList())
    val destinosCatalogo: StateFlow<List<DestinoCatalogo>> = _destinosCatalogo.asStateFlow()

    private val _camion = MutableStateFlow<Camion?>(null)
    val camion: StateFlow<Camion?> = _camion.asStateFlow()

    private val _cargasCombustible = MutableStateFlow<List<CargaCombustible>>(emptyList())
    val cargasCombustible: StateFlow<List<CargaCombustible>> = _cargasCombustible.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _incidenteReportado = MutableStateFlow(false)
    val incidenteReportado: StateFlow<Boolean> = _incidenteReportado.asStateFlow()

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val viajeActual = repository.getViajeById(viajeId)
                _viaje.value = viajeActual
                _destinos.value = repository.getDestinos(viajeId)
                _destinosCatalogo.value = repository.getDestinosCatalogo()
                _cargasCombustible.value = repository.getCargasCombustible(viajeId)
                // Solo GDE/MED tienen camión de catálogo (RENTA/Otro no) — necesario
                // para poder comparar el km inicial contra kilometrajeUltimoServicio.
                viajeActual?.camionId?.let { _camion.value = repository.getCamionById(it) }
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Error cargando viaje: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun destinoActual(): Destino? = _destinos.value.firstOrNull { it.fechaLlegada == null }

    fun iniciarDestino(destino: Destino, kmInicial: Int, canastillasIniciales: Int? = null) {
        // Red de seguridad además de la validación en la UI: un camión no
        // puede "retroceder" kilómetros entre dos destinos del mismo viaje.
        val anterior = _destinos.value.firstOrNull { it.orden == destino.orden - 1 }
        if (anterior?.kmFinal != null && kmInicial < anterior.kmFinal) {
            _error.value = "El km inicial no puede ser menor al km final del destino anterior (${anterior.kmFinal})"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.updateDestino(
                    viajeId,
                    destino.copy(fechaSalida = Timestamp(Date()), kmInicial = kmInicial)
                )
                // Con cuántas canastillas sale el camión de la matriz se captura
                // una sola vez, junto con el km inicial del primer destino.
                if (destino.orden == 0 && canastillasIniciales != null) {
                    _viaje.value?.let { repository.updateViaje(it.copy(canastillasIniciales = canastillasIniciales)) }
                }
                cargarDatos()
            } catch (e: Exception) {
                _error.value = "Error al iniciar destino: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registrarLlegada(
        destino: Destino,
        kmFinal: Int,
        canastillasEntregadas: Int,
        canastillasRegresadas: Int,
        fotoURL: String? = null
    ) {
        val kmInicial = destino.kmInicial
        if (kmInicial == null || kmFinal <= kmInicial) {
            _error.value = "El km final debe ser mayor al km inicial"
            return
        }

        // Red de seguridad además de la validación en la UI: no se pueden
        // entregar más canastillas de las que trae disponibles el camión
        // (iniciales - entregadas + regresadas de los destinos ya completados).
        _viaje.value?.canastillasIniciales?.let { iniciales ->
            val previos = _destinos.value.filter { it.orden < destino.orden }
            val disponibles = iniciales - previos.sumOf { it.canastillasEntregadas ?: 0 } +
                previos.sumOf { it.canastillasRegresadas ?: 0 }
            if (canastillasEntregadas > disponibles) {
                _error.value = "No puedes entregar más canastillas de las que trae el camión ($disponibles disponibles)"
                return
            }
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.updateDestino(
                    viajeId,
                    destino.copy(
                        fechaLlegada = Timestamp(Date()),
                        kmFinal = kmFinal,
                        canastillasEntregadas = canastillasEntregadas,
                        canastillasRegresadas = canastillasRegresadas,
                        fotoURL = fotoURL
                    )
                )

                val esUltimoDestino = destino.orden == _destinos.value.maxOf { it.orden }
                if (esUltimoDestino) {
                    _viaje.value?.let { repository.updateViaje(it.copy(concluido = true)) }
                }

                cargarDatos()
            } catch (e: Exception) {
                _error.value = "Error al registrar llegada: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========== EDICIÓN DE ITINERARIO (solo antes de iniciar el viaje) ==========
    // El chofer puede olvidar un destino al llenar el checklist; mientras
    // ningún destino tenga fechaSalida (el viaje aún no arrancó), se permite
    // agregar/quitar destinos igual que en ChecklistScreen.

    fun agregarDestinoAlItinerario(destinoCatalogo: DestinoCatalogo) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val nuevoOrden = _destinos.value.size
                repository.addDestino(
                    viajeId,
                    Destino(orden = nuevoOrden, cedisDestino = destinoCatalogo.nombre)
                )
                _destinos.value = repository.getDestinos(viajeId)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Error al agregar destino: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun eliminarDestinoDelItinerario(destino: Destino) {
        if (_destinos.value.size <= 1) {
            _error.value = "El itinerario debe tener al menos 1 destino"
            return
        }
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.deleteDestino(viajeId, destino.id)

                // Renumerar los restantes para que "orden" siga siendo 0..n-1
                // contiguo, igual que cuando se arma el itinerario original.
                val restantes = repository.getDestinos(viajeId)
                restantes.forEachIndexed { index, d ->
                    if (d.orden != index) {
                        repository.updateDestino(viajeId, d.copy(orden = index))
                    }
                }

                _destinos.value = repository.getDestinos(viajeId)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Error al eliminar destino: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun agregarCargaCombustible(
        ubicacion: String,
        kilometraje: Int,
        costoPorLitro: Float,
        litros: Float
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.addCargaCombustible(
                    viajeId,
                    CargaCombustible(
                        ubicacion = ubicacion,
                        kilometraje = kilometraje,
                        costoPorLitro = costoPorLitro,
                        litros = litros,
                        fechaCarga = Timestamp(Date())
                    )
                )
                _cargasCombustible.value = repository.getCargasCombustible(viajeId)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Error al agregar carga de combustible: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun reportarIncidente(descripcion: String, fotoURL: String) {
        val v = _viaje.value ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val chofer = repository.getChoferById(v.choferId)
                repository.addIncidente(
                    Incidente(
                        viajeId = viajeId,
                        choferId = v.choferId,
                        choferNombre = chofer?.nombre ?: "",
                        placa = v.placa,
                        descripcion = descripcion,
                        fotoURL = fotoURL,
                        fecha = Timestamp(Date()),
                        estado = "Pendiente"
                    )
                )
                _incidenteReportado.value = true
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Error al reportar problema: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun limpiarIncidenteReportado() {
        _incidenteReportado.value = false
    }

    fun limpiarError() {
        _error.value = null
    }
}
