# ChecklistChofer - Progress & Status

**Última actualización**: 2026-08-23 (noche)  
**Demo objetivo**: Lunes 2026-08-25  
**Estado general**: Flujo Android completo (Pantalla 1 + Checklist + Bitácora/Pantalla 2 + fotos); falta desktop

---

## 📊 Overview por Componente

| Componente | Estado | % Completo |
|-----------|--------|-----------|
| **Android App** | ✅ Flujo completo (chofer → checklist → bitácora → fotos) | 95% |
| **Firebase Backend** | ✅ Listo (Firestore + Storage con plan Blaze) | 100% |
| **Desktop App (Java)** | ❌ No iniciado | 0% |
| **Fotos/Storage** | ❌ No implementado | 0% |
| **Botones especiales** | ✅ Listo | 100% |

---

## ✅ Completado en Esta Sesión (2026-08-23)

### Data Layer
- [x] CheckField: `valor: String` (soporta "BIEN", "MAL", "N/A", "SÍ", "NO")
- [x] InspeccionGeneral: 18 campos correctos y en orden
- [x] Viaje: tipoUnidad, placa, economico, camionId (nullable)
- [x] Camion: economico (No. de Unidad / Económico)
- [x] Firebase Repository: CRUD operacional

### Formulario Inicial (ViajeScreen) - 85% Completo ✅
- [x] Fecha automática (no editable) - visible
- [x] Selector de Chofer - funcional
- [x] Tipo de Unidad (GDE/MED/RENTA/Otro) - funcional
- [x] Filtrado dinámico de Placas por tipo:
  - [x] GDE/MED: lista de camiones con placa
  - [x] RENTA: lista predefinida (RENTA001-RENTA006)
  - [x] Otro: campo de texto manual
- [x] Para RENTA: "Detalle Renta" (dropdown marcas: Ford, Toyota, Caja, Redila, Batea, Volteo)
- [x] No. de Unidad / Económico: ingreso manual (todos los tipos)
- [x] Validaciones completas
- [x] Scroll vertical funcional
- [x] Campos guardando correctamente en Firestore (placa, economico, tipoUnidad)

### Validación & Lógica
- [x] N/A es opción válida que permite guardar
- [x] Presión de llantas: solo números (KeyboardType.Decimal)
- [x] Observaciones opcionales en todas las preguntas
- [x] Rendimiento combustible: valida tanque lleno salida/regreso = "SÍ"
- [x] Catálogos se crean automáticamente si Firestore vacío

### UI - Pantalla Checklist
- [x] 18 campos inspección en orden correcto
- [x] Opciones: **BIEN / MAL / N/A** (inspección)
- [x] Opciones: **SÍ / NO** (combustible, limpieza, documentación)
- [x] Presión llantas en posición 3 (tabla dinámica)
- [x] Nivel Urea en posición 11 (slider 0-100%)
- [x] Combustible Thermo en posición 12 (dropdown)
- [x] Itinerario: agregar/eliminar destinos (bloquea post-guardado)
- [x] Botones SÍ/NO/BIEN/MAL/N/A destacados visualmente
- [x] Sin números en etiquetas de preguntas
- [x] Sin "18 campos" en título

### Firebase & Persistencia
- [x] Firestore Database configurado (proyecto checklist-choferes)
- [x] Colecciones: choferes, camiones, destinosCatalogo, viajes
- [x] Subcolecciones: viajes/{id}/destinos, cargasCombustible
- [x] Soft-delete (bool activo/inactivo)
- [x] Google Services plugin integrado
- [x] Datos guardando correctamente en BD

---

## ⏳ Pendiente - Prioridad Alto

### Android App - ViajeScreen (Pantalla Inicial) - UI Polish ✅ COMPLETO (2026-08-23)
- [x] Placeholders correctos en todos los campos ("Seleccionar nombre", "Seleccionar tipo de unidad", "Seleccionar placa/placas", "Seleccionar detalle", "Ingresar dato")
- [x] Bordes redondeados unificados (RoundedCornerShape(8.dp) en dropdowns y text fields)
- [x] Mismo alto (56.dp) en dropdowns y text fields
- [x] Sin preselección automática de chofer/tipo/camión al abrir la app
- [x] Enter/imeAction Done en campos de texto (No. de Unidad, Placa Otro) — antes causaba salto de línea en campo de altura fija
- [x] Scroll funcional con teclado abierto (imePadding)
- [x] Catálogos de prueba ampliados: 2 choferes, 2 camiones GDE, 2 camiones MED (para poder probar dropdowns con opciones reales)

### Android App - Pantalla 2: Control de Viaje ✅ IMPLEMENTADO (2026-08-23, pendiente confirmación en dispositivo)
- [x] Fix: al guardar el checklist (Pantalla 1) ahora sí se crean los subdocumentos `viajes/{id}/destinos` (antes solo se guardaba una lista de IDs en el documento del viaje, y Pantalla 2 no tenía de dónde leer)
- [x] **Itinerario congelado**: muestra lista de destinos con orden y estado (Pendiente / En curso / Completado)
- [x] **Botones dinámicos por destino**:
  - [x] Si sin `fechaSalida` → **"Iniciar viaje a [DESTINO]"**
    - [x] Modal: ingresa kmInicial
    - [x] Registra fechaSalida (automática)
    - [x] Guarda en subcolección destinos
  - [x] Si con `fechaSalida` sin `fechaLlegada` → **"Ya llegué a [DESTINO]"**
    - [x] Modal: ingresa kmFinal, canastillas entregadas/regresadas, nota opcional
    - [ ] Foto opcional (pendiente, depende de sección Fotos/Storage)
    - [x] Registra fechaLlegada (automática)
    - [x] Valida: kmFinal > kmInicial
    - [x] Marca viaje.concluido = true si era último destino
  - [x] **"+ Agregar carga de combustible"** (botón siempre visible)
    - [x] Modal: ubicación, km, $/litro, litros
    - [x] Registra fechaCarga (automática)
    - [x] Permite múltiples cargas
- [ ] **Pendiente confirmar en dispositivo**: flujo completo iniciar→llegar→combustible→concluir

### Android App - Fotos ✅ COMPLETO (2026-08-23, confirmado en dispositivo)
- [x] Botón cámara en cada observación (22 campos: Combustible/Limpieza, Inspección General, Documentación) + nota de llegada por destino
- [x] Solo cámara (no galería) — Intent ACTION_IMAGE_CAPTURE vía FileProvider
- [x] Upload a Firebase Storage (requirió activar plan Blaze del proyecto — Storage ya no está en el plan gratuito Spark)
- [x] Mostrar URL en BD (fotoURL guardado en cada CheckField / Destino en Firestore)

---

## ❌ No Iniciado - Fase Posterior

### Desktop App (Java + Swing)
- [ ] Proyecto Gradle inicial
- [ ] Dependencies: Swing, Apache POI
- [ ] Dashboard principal con panel de control
- [ ] **Filtros**: fecha, placa, chofer, destino
- [ ] **CRUD Catálogos**:
  - [ ] Agregar/editar/eliminar choferes
  - [ ] Agregar/editar/eliminar camiones
  - [ ] Agregar/editar/eliminar destinos
  - [ ] Soft-delete (bool activo → inactivo)
- [ ] **Visualización de viajes**:
  - [ ] Filtros aplicados
  - [ ] Estado: activo vs concluido
  - [ ] Datos en tabla
- [ ] **Generación de reportes**:
  - [ ] Leer datos de Firestore
  - [ ] Llenar plantilla Word (Apache POI)
  - [ ] Exportar/imprimir PDF
- [ ] **Métricas**:
  - [ ] Rendimiento combustible: (KM_final_último - KM_inicial_primero) / sum(litros)
  - [ ] Válido solo si tanque salió y regresó lleno
  - [ ] Alerta servicio: si (km_salida - kilometrajeUltimoServicio) ≥ 9000

---

## 📋 Inspección General - 18 Campos

| # | Campo | Opciones | Validación |
|---|-------|----------|-----------|
| 1 | Llantas (desgaste) | BIEN/MAL/N/A | Obs. opcional |
| 2 | Llanta de refacción | BIEN/MAL/N/A | Obs. opcional |
| 3 | **Presión de llantas** | [tabla dinámica] | **Obligatorio** |
| 4 | Sistema frenado óptimo | BIEN/MAL/N/A | Obs. opcional |
| 5 | Luces (altas, bajas, dir., reversa, stop) | BIEN/MAL/N/A | Obs. opcional |
| 6 | Espejos laterales y retrovisor | BIEN/MAL/N/A | Obs. opcional |
| 7 | Limpiaparabrisas y claxon | BIEN/MAL/N/A | Obs. opcional |
| 8 | Nivel de aceite de motor | BIEN/MAL/N/A | Obs. opcional |
| 9 | Nivel de agua / refrigerante | BIEN/MAL/N/A | Obs. opcional |
| 10 | Nivel de líquido de frenos | BIEN/MAL/N/A | Obs. opcional |
| 11 | **Nivel de Urea** | [slider 0-100%] | **Obligatorio** |
| 12 | **Nivel combustible thermo** | [dropdown] | **Obligatorio** |
| 13 | Batería (terminales y carga) | BIEN/MAL/N/A | Obs. opcional |
| 14 | Triángulos de seguridad | BIEN/MAL/N/A | Obs. opcional |
| 15 | Gato hidráulico, cruceta, herramienta | BIEN/MAL/N/A | Obs. opcional |
| 16 | Estado de Carrocería (golpes, rayones) | BIEN/MAL/N/A | Obs. opcional |
| 17 | Candados de caja | BIEN/MAL/N/A | Obs. opcional |
| 18 | Bandas de seguridad para la caja | BIEN/MAL/N/A | Obs. opcional |

---

## 🎯 Roadmap para Demo (Lunes 2026-08-25)

### Mínimo Viable (CRÍTICO)
1. ✅ Iniciar viaje (chofer, tipo, placa, número)
2. ✅ Llenar checklist completo (18 campos)
3. ✅ Guardar en Firestore
4. ✅ Botones de destinos (iniciar/terminar)
5. ✅ Botón agregar carga combustible

### Nice-to-Have
6. ✅ Fotos en observaciones
7. ❌ Desktop app
8. ❌ Generación de Word

**Plan**: Android funcional completo, incluyendo fotos. Desktop = fase 2.

---

## 🔧 Arquitectura Técnica

### Android Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose
- **State Management**: StateFlow + ViewModel
- **Database**: Firebase Firestore
- **Auth**: Firebase Auth (básico)
- **Target SDK**: 26+

### Data Model
```
Viaje (documento)
├── Datos básicos (chofer, camión, fecha, etc)
├── CombustibleYLimpieza (3 campos SÍ/NO)
├── InspeccionGeneral (15 campos BIEN/MAL/N/A + Urea + Thermo)
├── DocumentacionEquipo (4 campos SÍ/NO)
├── PresionLlantas[] (tabla dinámica)
├── ObservacionesGenerales (string)
└── Subcolecciones:
    ├── destinos/{id}
    │   ├── cedisDestino, km, fechas
    │   ├── canastillas, nota, foto (NO imprime)
    └── cargasCombustible/{id}
        ├── ubicacion, km, $/litro, litros, fecha
```

### Firebase Rules (Modo Prueba)
- Lectura/escritura permitida para todos (desarrollo)
- Seguridad en producción: implementar auth + rules

---

## 📝 Notas & Decisiones Técnicas

1. **CheckField con String**: Cambio de Boolean? a String permite diferenciar "no tocado" ("") de opciones válidas
2. **N/A como opción**: Válido en inspección (algunos items no aplican a ciertos camiones)
3. **Presión dinámica**: Cantidad de llantas depende de catálogo (GDE/MED/RENTA vs Otro)
4. **Soft-delete**: Datos históricos nunca se borran, solo marcados inactivo
5. **No hay captura de firma digital**: Firma a mano en documento impreso

---

## 🚀 Próximos Pasos (Cuando retomes)

**Sesión siguiente**:
1. Arreglar ViajeScreen (tipo + fecha auto)
2. Implementar botones de destinos
3. Implementar agregar cargas
4. Prueba e2e del flujo completo
5. (Opcional) Fotos con Storage
6. (Optional) Empezar desktop si hay tiempo

