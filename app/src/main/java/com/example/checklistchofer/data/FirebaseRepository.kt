package com.example.checklistchofer.data

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val db: FirebaseFirestore = Firebase.firestore
    private val storage = Firebase.storage

    // ========== FOTOS (STORAGE) ==========

    suspend fun subirFoto(localUri: Uri, storagePath: String): String {
        val ref = storage.reference.child(storagePath)
        ref.putFile(localUri).await()
        return ref.downloadUrl.await().toString()
    }

    // ========== INICIALIZACIÓN ==========

    suspend fun initializeCollections() {
        try {
            // Crear colecciones si no existen
            // En Firestore, una colección existe cuando tiene al menos 1 documento
            createChoferes()
            createCamiones()
            createDestinosCatalogo()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ========== CATÁLOGOS ==========

    private suspend fun createChoferes() {
        val choferes = db.collection("choferes")
        val seed = listOf(
            Chofer(id = "chofer_001", nombre = "Jose Canela", activo = true),
            Chofer(id = "chofer_002", nombre = "Maria Torres", activo = true)
        )
        for (chofer in seed) {
            val doc = choferes.document(chofer.id).get().await()
            if (!doc.exists()) {
                choferes.document(chofer.id).set(chofer).await()
            }
        }
    }

    private suspend fun createCamiones() {
        val camiones = db.collection("camiones")
        val posicionesEstandar = listOf("Delantera Izq", "Delantera Der", "Trasera Izq", "Trasera Der")
        val seed = listOf(
            Camion(
                id = "camion_001",
                placa = "ABC123",
                tipo = "GDE",
                kilometrajeUltimoServicio = 50000,
                posicionesLlantas = posicionesEstandar,
                activo = true
            ),
            Camion(
                id = "camion_002",
                placa = "DEF456",
                tipo = "GDE",
                kilometrajeUltimoServicio = 62000,
                posicionesLlantas = posicionesEstandar,
                activo = true
            ),
            Camion(
                id = "camion_003",
                placa = "GHI789",
                tipo = "MED",
                kilometrajeUltimoServicio = 38000,
                posicionesLlantas = posicionesEstandar,
                activo = true
            ),
            Camion(
                id = "camion_004",
                placa = "JKL012",
                tipo = "MED",
                kilometrajeUltimoServicio = 71000,
                posicionesLlantas = posicionesEstandar,
                activo = true
            )
        )
        for (camion in seed) {
            val doc = camiones.document(camion.id).get().await()
            if (!doc.exists()) {
                camiones.document(camion.id).set(camion).await()
            }
        }
    }

    private suspend fun createDestinosCatalogo() {
        val destinos = db.collection("destinosCatalogo")
        val snapshot = destinos.limit(1).get().await()

        if (!snapshot.isEmpty) return

        val destinosEjemplo = listOf(
            DestinoCatalogo(id = "dest_001", nombre = "CEDIS Centro", activo = true),
            DestinoCatalogo(id = "dest_002", nombre = "CEDIS Norte", activo = true),
            DestinoCatalogo(id = "dest_003", nombre = "CEDIS Sur", activo = true)
        )

        for (destino in destinosEjemplo) {
            destinos.document(destino.id).set(destino).await()
        }
    }

    // ========== OPERACIONES CRUD: CHOFERES ==========

    suspend fun addChofer(chofer: Chofer): String {
        val docRef = db.collection("choferes").document()
        val choferId = docRef.id
        docRef.set(chofer.copy(id = choferId)).await()
        return choferId
    }

    suspend fun getChoferes(): List<Chofer> {
        return db.collection("choferes")
            .whereEqualTo("activo", true)
            .get()
            .await()
            .toObjects(Chofer::class.java)
    }

    suspend fun getChoferById(id: String): Chofer? {
        return db.collection("choferes")
            .document(id)
            .get()
            .await()
            .toObject(Chofer::class.java)
    }

    suspend fun updateChofer(chofer: Chofer) {
        db.collection("choferes")
            .document(chofer.id)
            .set(chofer)
            .await()
    }

    suspend fun deleteChofer(id: String) {
        // Soft-delete: marca como inactivo, no borra
        db.collection("choferes")
            .document(id)
            .update("activo", false)
            .await()
    }

    // ========== OPERACIONES CRUD: CAMIONES ==========

    suspend fun addCamion(camion: Camion): String {
        val docRef = db.collection("camiones").document()
        val camionId = docRef.id
        docRef.set(camion.copy(id = camionId)).await()
        return camionId
    }

    suspend fun getCamiones(): List<Camion> {
        return db.collection("camiones")
            .whereEqualTo("activo", true)
            .get()
            .await()
            .toObjects(Camion::class.java)
    }

    suspend fun getCamionById(id: String): Camion? {
        return db.collection("camiones")
            .document(id)
            .get()
            .await()
            .toObject(Camion::class.java)
    }

    suspend fun updateCamion(camion: Camion) {
        db.collection("camiones")
            .document(camion.id)
            .set(camion)
            .await()
    }

    suspend fun deleteCamion(id: String) {
        db.collection("camiones")
            .document(id)
            .update("activo", false)
            .await()
    }

    // ========== OPERACIONES CRUD: DESTINOS CATÁLOGO ==========

    suspend fun addDestinoCatalogo(destino: DestinoCatalogo): String {
        val docRef = db.collection("destinosCatalogo").document()
        val destinoId = docRef.id
        docRef.set(destino.copy(id = destinoId)).await()
        return destinoId
    }

    suspend fun getDestinosCatalogo(): List<DestinoCatalogo> {
        return db.collection("destinosCatalogo")
            .whereEqualTo("activo", true)
            .get()
            .await()
            .toObjects(DestinoCatalogo::class.java)
    }

    suspend fun updateDestinoCatalogo(destino: DestinoCatalogo) {
        db.collection("destinosCatalogo")
            .document(destino.id)
            .set(destino)
            .await()
    }

    suspend fun deleteDestinoCatalogo(id: String) {
        db.collection("destinosCatalogo")
            .document(id)
            .update("activo", false)
            .await()
    }

    // ========== OPERACIONES CRUD: VIAJES ==========

    suspend fun createViaje(viaje: Viaje): String {
        val docRef = db.collection("viajes").document()
        val viajeId = docRef.id
        docRef.set(viaje.copy(id = viajeId)).await()
        return viajeId
    }

    suspend fun getViajeById(id: String): Viaje? {
        return db.collection("viajes")
            .document(id)
            .get()
            .await()
            .toObject(Viaje::class.java)
    }

    suspend fun updateViaje(viaje: Viaje) {
        db.collection("viajes")
            .document(viaje.id)
            .set(viaje)
            .await()
    }

    suspend fun getViajesByChofer(choferId: String): List<Viaje> {
        return db.collection("viajes")
            .whereEqualTo("choferId", choferId)
            .orderBy("fecha")
            .get()
            .await()
            .toObjects(Viaje::class.java)
    }

    suspend fun getViajesByDate(startDate: com.google.firebase.Timestamp, endDate: com.google.firebase.Timestamp): List<Viaje> {
        return db.collection("viajes")
            .whereGreaterThanOrEqualTo("fecha", startDate)
            .whereLessThanOrEqualTo("fecha", endDate)
            .get()
            .await()
            .toObjects(Viaje::class.java)
    }

    // ========== SUBCOLECCIÓN: DESTINOS ==========

    suspend fun addDestino(viajeId: String, destino: Destino): String {
        val docRef = db.collection("viajes")
            .document(viajeId)
            .collection("destinos")
            .document()

        val destinoId = docRef.id
        docRef.set(destino.copy(id = destinoId)).await()
        return destinoId
    }

    suspend fun getDestinos(viajeId: String): List<Destino> {
        return db.collection("viajes")
            .document(viajeId)
            .collection("destinos")
            .orderBy("orden")
            .get()
            .await()
            .toObjects(Destino::class.java)
    }

    suspend fun updateDestino(viajeId: String, destino: Destino) {
        db.collection("viajes")
            .document(viajeId)
            .collection("destinos")
            .document(destino.id)
            .set(destino)
            .await()
    }

    // ========== SUBCOLECCIÓN: CARGAS DE COMBUSTIBLE ==========

    suspend fun addCargaCombustible(viajeId: String, carga: CargaCombustible): String {
        val docRef = db.collection("viajes")
            .document(viajeId)
            .collection("cargasCombustible")
            .document()

        val cargaId = docRef.id
        docRef.set(carga.copy(id = cargaId)).await()
        return cargaId
    }

    suspend fun getCargasCombustible(viajeId: String): List<CargaCombustible> {
        return db.collection("viajes")
            .document(viajeId)
            .collection("cargasCombustible")
            .orderBy("fechaCarga")
            .get()
            .await()
            .toObjects(CargaCombustible::class.java)
    }

    // ========== COLECCIÓN: INCIDENTES ==========

    suspend fun addIncidente(incidente: Incidente): String {
        val docRef = db.collection("incidentes").document()
        val id = docRef.id
        docRef.set(incidente.copy(id = id)).await()
        return id
    }

    // ========== MÉTRICAS ==========

    suspend fun calcularRendimientoCombustible(viajeId: String): Float {
        val viaje = getViajeById(viajeId) ?: return 0f
        val destinos = getDestinos(viajeId)
        val cargas = getCargasCombustible(viajeId)

        if (destinos.isEmpty() || cargas.isEmpty()) return 0f

        // Solo válido si tanque salió y regresó lleno
        if (viaje.combustibleYLimpieza.tanqueLlenoSalida.valor == "SÍ" &&
            viaje.combustibleYLimpieza.tanqueLlenoRegreso.valor == "SÍ"
        ) {
            val kmInicial = destinos.first().kmInicial
            val kmFinal = destinos.last().kmFinal
            val totalLitros = cargas.sumOf { it.litros.toDouble() }.toFloat()

            return if (kmInicial != null && kmFinal != null && totalLitros > 0) {
                (kmFinal - kmInicial) / totalLitros
            } else {
                0f
            }
        }

        return 0f
    }

    suspend fun verificarAlertaServicio(camionId: String): Boolean {
        val camion = getCamionById(camionId) ?: return false
        val viajes = db.collection("viajes")
            .whereEqualTo("camionId", camionId)
            .orderBy("fecha")
            .limit(1)
            .get()
            .await()
            .toObjects(Viaje::class.java)

        if (viajes.isEmpty()) return false

        val ultimoViaje = viajes.first()
        val destinos = getDestinos(ultimoViaje.id)
        if (destinos.isEmpty()) return false

        val kmSalida = destinos.first().kmInicial ?: return false
        val kmDiferencia = kmSalida - camion.kilometrajeUltimoServicio

        return kmDiferencia >= 9000
    }
}
