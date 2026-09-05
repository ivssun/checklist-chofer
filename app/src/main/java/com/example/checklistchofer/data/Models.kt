package com.example.checklistchofer.data

import com.google.firebase.Timestamp

// ========== CATÁLOGOS ==========

data class Chofer(
    val id: String = "",
    val nombre: String = "",
    val activo: Boolean = true
)

data class Camion(
    val id: String = "",
    val placa: String = "",
    val economico: String = "", // No. de Unidad / Económico
    val tipo: String = "", // "GDE", "MED", "RENTA", "Otro"
    val kilometrajeUltimoServicio: Long = 0,
    val posicionesLlantas: List<String> = emptyList(),
    val activo: Boolean = true
)

data class DestinoCatalogo(
    val id: String = "",
    val nombre: String = "",
    val activo: Boolean = true
)

// ========== VIAJE PRINCIPAL ==========

data class Viaje(
    val id: String = "",
    val choferId: String = "",
    val camionId: String? = null, // null si tipoUnidad es RENTA u Otro
    val tipoUnidad: String = "", // GDE, MED, RENTA, Otro
    val placa: String = "", // placa del vehículo
    val detalleRenta: String = "", // marca/modelo, solo aplica si tipoUnidad = RENTA
    val economico: String = "", // No. de Unidad / Económico
    val fecha: Timestamp? = null,
    val horaLlegadaMatriz: Timestamp? = null,
    val concluido: Boolean = false,
    val cancelado: Boolean = false, // el chofer eligió "Cancelar e iniciar un nuevo viaje" al reabrir la app con este viaje en curso

    val destinosSeleccionados: List<String> = emptyList(), // IDs de destinos del catálogo
    val canastillasIniciales: Int? = null, // con cuántas canastillas sale el camión de la matriz — se captura junto con el km inicial del primer destino (2026-09-05, feedback cliente real)
    val combustibleYLimpieza: CombustibleYLimpieza = CombustibleYLimpieza(),
    val inspeccionGeneral: InspeccionGeneral = InspeccionGeneral(),
    val presionLlantas: List<PresionLlanta> = emptyList(),
    val presionLlantasObservacion: ObservacionFoto = ObservacionFoto(),
    val ureaPorcentaje: Int = 0,
    val ureaObservacion: ObservacionFoto = ObservacionFoto(),
    val combustibleThermo: String = "", // "0", "1/4", "1/2", "3/4", "Lleno"
    val combustibleThermoObservacion: ObservacionFoto = ObservacionFoto(),
    val documentacionEquipo: DocumentacionEquipo = DocumentacionEquipo(),
    val observacionesGenerales: String = "",
    val observacionesGeneralesFotoURL: String? = null // foto opcional para reportar algo extraordinario no cubierto por el formulario
)

// ========== NESTED OBJECTS DENTRO DE VIAJE ==========

data class CombustibleYLimpieza(
    val tanqueLlenoSalida: CheckField = CheckField(),
    val tanqueLlenoRegreso: CheckField = CheckField(),
    val limpiezaCajaCabina: CheckField = CheckField()
)

data class CheckField(
    val valor: String = "", // "", "BIEN", "MAL", "N/A"
    val observacion: String = "",
    val fotoURL: String? = null
)

data class InspeccionGeneral(
    val llantasDesgaste: CheckField = CheckField(),                  // 1
    val llantaRefaccion: CheckField = CheckField(),                  // 2
    // 3: Presión de llantas (tabla separada en presionLlantas)
    val sistemaFrenado: CheckField = CheckField(),                   // 4
    val luces: CheckField = CheckField(),                            // 5
    val espejos: CheckField = CheckField(),                          // 6
    val limpiaparabrisas: CheckField = CheckField(),                 // 7
    val nivelAceite: CheckField = CheckField(),                      // 8
    val nivelAgua: CheckField = CheckField(),                        // 9
    val nivelLiquidoFreno: CheckField = CheckField(),                // 10
    // 11: Nivel Urea (slider separado en ureaPorcentaje)
    // 12: Combustible Thermo (dropdown separado en combustibleThermo)
    val bateria: CheckField = CheckField(),                          // 13
    val triangulos: CheckField = CheckField(),                       // 14
    val gato: CheckField = CheckField(),                             // 15
    val carroceria: CheckField = CheckField(),                       // 16
    val candados: CheckField = CheckField(),                         // 17
    val bandas: CheckField = CheckField()                            // 18
)

data class PresionLlanta(
    val etiqueta: String = "",
    val presion: Float = 0f
)

data class ObservacionFoto(
    val observacion: String = "",
    val fotoURL: String? = null
)

data class DocumentacionEquipo(
    val licenciaChofer: CheckField = CheckField(),
    val tarjetaCirculacion: CheckField = CheckField(),
    val segurosVehiculo: CheckField = CheckField(),
    val documentoViaje: CheckField = CheckField()
)

// ========== SUBCOLECCIÓN: DESTINOS ==========

data class Destino(
    val id: String = "",
    val orden: Int = 0,
    val cedisDestino: String = "",
    val kmInicial: Int? = null,
    val kmFinal: Int? = null,
    val fechaSalida: Timestamp? = null,
    val fechaLlegada: Timestamp? = null,
    val canastillasEntregadas: Int? = null,
    val canastillasRegresadas: Int? = null,
    val nota: String? = null,
    val fotoURL: String? = null
)

// ========== SUBCOLECCIÓN: CARGAS DE COMBUSTIBLE ==========

data class CargaCombustible(
    val id: String = "",
    val ubicacion: String = "",
    val kilometraje: Int = 0,
    val costoPorLitro: Float = 0f,
    val litros: Float = 0f,
    val fechaCarga: Timestamp? = null
)

// ========== COLECCIÓN: INCIDENTES (nivel raíz, no subcolección) ==========

data class Incidente(
    val id: String = "",
    val viajeId: String = "",
    val choferId: String = "",
    val choferNombre: String = "", // desnormalizado para la tabla del Supervisor
    val placa: String = "",        // desnormalizado, misma razón
    val descripcion: String = "",
    val fotoURL: String = "",
    val fecha: Timestamp? = null,
    val estado: String = "Pendiente", // "Pendiente" | "Resuelto"
    val fechaResuelto: Timestamp? = null
)
