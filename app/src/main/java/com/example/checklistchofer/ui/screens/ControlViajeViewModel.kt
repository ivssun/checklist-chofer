package com.example.checklistchofer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.checklistchofer.data.CargaCombustible
import com.example.checklistchofer.data.Destino
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
                _viaje.value = repository.getViajeById(viajeId)
                _destinos.value = repository.getDestinos(viajeId)
                _cargasCombustible.value = repository.getCargasCombustible(viajeId)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Error cargando viaje: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun destinoActual(): Destino? = _destinos.value.firstOrNull { it.fechaLlegada == null }

    fun iniciarDestino(destino: Destino, kmInicial: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.updateDestino(
                    viajeId,
                    destino.copy(fechaSalida = Timestamp(Date()), kmInicial = kmInicial)
                )
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
