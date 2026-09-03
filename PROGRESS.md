# ChecklistChofer - Progress & Status

**Última actualización**: 2026-09-01  
**Demo objetivo**: Lunes 2026-08-25 (ya pasada) — ahora preparando demo con **cliente real**  
**Estado general**: Android completo, incluyendo el feedback del cliente real (autoguardado extendido + reporte de incidentes + ajustes UX), confirmado en dispositivo. App Supervisor (web): **completa** — Dashboard con métricas, Detalle de viaje, vista imprimible con formato Word real, CRUD de catálogos y tema de marca Panissimo, todo confirmado funcionando por el usuario y en vivo en https://checklist-choferes.web.app.

### Sesión 2026-09-01 (continuación): App Supervisor arranca — Dashboard v1
- [x] Proyecto Vite + React (JavaScript, sin TypeScript) creado en `supervisor-web/` — carpeta hermana de `app/`, dentro del mismo repo (monorepo simple, no repo aparte)
- [x] IDE: VS Code (no Android Studio) — abrir `ChecklistChofer/` como carpeta raíz en VS Code, no solo `supervisor-web/`, para que sea visible este mismo CLAUDE.md/PROGRESS.md si se abre una sesión de Claude Code ahí
- [x] Dependencias instaladas: `firebase` (Web SDK), `@mantine/core` + `@mantine/hooks` (librería de componentes, decisión ya tomada — no shadcn/Tailwind) + `postcss-preset-mantine`
- [x] Web App registrada en la consola de Firebase (proyecto `checklist-choferes`) — config guardada en `supervisor-web/src/firebase.js` (Firestore + Storage inicializados; la apiKey de Firebase Web no es secreta, es seguro tenerla en el código de cliente/repo)
- [x] Dashboard v1 (`supervisor-web/src/pages/Dashboard.jsx`), confirmado funcionando por el usuario en `http://localhost:5173`:
  - [x] Lista de viajes reales desde Firestore (fecha, chofer, tipo, placa, económico, destinos, estado)
  - [x] Filtros: chofer, placa, destino (dropdowns con búsqueda), rango de fecha (desde/hasta)
  - [x] Badge Activo/Concluido por viaje
  - [x] Alerta roja arriba con conteo y detalle de `incidentes` con `estado: "Pendiente"` (colección nueva, alimentada desde Android)
- [x] Cambios de `supervisor-web/` subidos a GitHub
- [x] **Deploy de prueba a Firebase Hosting — CONFIRMADO EN VIVO**: https://checklist-choferes.web.app (público, gratis dentro de la capa gratuita — ver nota de costos abajo)
  - `firebase-tools` instalado global en la laptop, sesión logueada con `olimpiamoctezuma@gmail.com` — deploys futuros no piden login de nuevo
  - Config: `supervisor-web/firebase.json` (public: `dist`, rewrite SPA a `index.html`) + `supervisor-web/.firebaserc` (proyecto `checklist-choferes`)
  - **Para volver a desplegar cambios**: dentro de `supervisor-web/`, `npm run build` seguido de `firebase deploy --only hosting`
  - Costo real: $0 — Hosting no es un servidor "prendido" que cobre por tiempo activo, solo cobra por almacenamiento/transferencia si se excede la capa gratuita (10GB/360MB por día), muy lejos del uso de esta demo
- **Próximo paso cuando se retome**: botón "Marcar como resuelto" en los incidentes, página de detalle de viaje (con métricas de rendimiento/alerta de servicio, que requieren leer subcolecciones `destinos`/`cargasCombustible`), CRUD de catálogos, vista imprimible — y volver a desplegar cuando haya avances

### Sesión 2026-09-01: Feedback del cliente real (pre-demo)
- [x] Android — Autoguardado `ViajeScreen`: borrador local (SharedPreferences) de los campos antes de crear el doc del viaje, restaurado al reabrir — confirmado funcionando por el usuario
- [x] Android — Autoguardado `ChecklistScreen`: cada respuesta se escribe directo a Firestore (debounced 600ms) al capturarla, en vez de esperar al botón final — confirmado funcionando
- [x] Android — Botón "🚨 Reportar problema" en `ControlViajeScreen` (con viaje activo): descripción + foto obligatoria → colección `incidentes` (`estado: "Pendiente"`) — confirmado funcionando
- [x] Android — Fix UX: texto del botón de foto decía "(opcional)" incluso cuando la foto es obligatoria (Reportar problema) — ahora `BotonFotoCamara` acepta `obligatoria: Boolean` y ajusta el texto
- [x] Android — Botón "Siguiente" del teclado encadenado en los diálogos "Ya llegué a [destino]" (Km final → Canastillas entregadas → Canastillas regresadas) y "Agregar carga de combustible" (Ubicación → Km → Costo/L → Litros), antes todos mostraban "Listo" — confirmado funcionando
- [x] Supervisor (web) — Sección "Problemas pendientes" en el Dashboard: alerta con lista de incidentes — confirmado funcionando (falta el botón "Marcar como resuelto", ver sesión de abajo)
- Ver detalle completo de las features en CLAUDE.md → "Autoguardado de progreso (2026-09-01)", "Reporte de incidentes" (Pantalla 2), "Colección incidentes", "Problemas pendientes" (Supervisor)
- Cambio de prioridad: para la app Supervisor, el cliente pidió buena presentación visual (antes se priorizaba solo velocidad/funcionalidad) — usar librería de componentes gratuita (Mantine o shadcn/ui + Tailwind)
- **Android: las 3 features del feedback del cliente están completas y confirmadas en dispositivo. No queda pendiente nada más de Android para esta demo — el siguiente bloque de trabajo es la app Supervisor (web), que sigue sin arrancar.**

### Sesión 2026-08-25: Feedback de QA (Jose Canela)
- [x] Dropdown de Nombre: ahora filtra por texto al escribir (antes solo lista sin buscar) — confirmado funcionando por el usuario
- [x] Tipo de Unidad: opciones renombradas a "ISUZU GDE"/"ISUZU MED"/"ISUZU RENTA"/"OTRO" (solo etiqueta visual, el valor guardado en Firestore no cambió) — confirmado funcionando
- [x] ISUZU RENTA: reordenado — ahora pide primero "Detalle Renta" (placeholder "Seleccionar dato") y luego "Placas"
- [x] OTRO: ahora pide "Marca" (texto libre) y "Placas" (texto libre), antes solo pedía placa y perdía el dato de marca — confirmado funcionando
- [x] Tarjeta "Información del Camión": título resaltado (color de marca + tipografía); se quitó la línea "Posiciones de llantas" (dato interno sin sentido para el chofer, solo se usa internamente en la tabla de presión de llantas) — confirmado funcionando
- [x] Fix de bug: la app se reiniciaba en Pantalla 1 si se cerraba después de "Iniciar Viaje", aunque el viaje ya existía en Firestore (huérfano, sin checklist). Ahora se persiste sesión (viajeId + pantalla) en SharedPreferences — confirmado funcionando por el usuario
- [x] Pantalla "Viaje en curso" al reabrir la app: en vez de retomar directo, pregunta "Continuar con el viaje en curso" / "Cancelar e iniciar un nuevo viaje" (por si cancelaron el viaje o cambiaron de unidad mientras la app estaba cerrada) — pendiente de confirmar por el usuario
- [x] Placeholders de Pantalla 1 en rojo (color error) para que resalte visualmente qué campos faltan por llenar — pendiente de confirmar por el usuario
- [x] Revisado en consola de Firebase: colección `viajes` tenía 55 documentos, solo 1 concluido y 1 huérfano válido — el resto basura de pruebas. Se borró toda la colección `viajes` (con subcolecciones `destinos`/`cargasCombustible`) para arrancar limpio antes de la app Supervisor. Catálogos (`choferes`, `camiones`, `destinosCatalogo`) no se tocaron.
- [x] Fix de robustez: si el viaje que se intenta retomar (Checklist o Control de Viaje) ya no existe en Firestore (p. ej. se borró desde la consola), la app regresa sola a Pantalla 1 en vez de quedar en una pantalla en blanco sin salida (`onViajeNoEncontrado` en ambas pantallas)
- [x] App renombrada de "ChecklistChofer" a "Checklist" en el ícono (no cabía completo)
- [x] Nombre del colaborador: corregido para que un solo toque abra teclado + lista filtrable juntos (causa raíz: el menú desplegable era focusable y le robaba el foco de ventana al campo de texto, cerrando el teclado) — confirmado funcionando
- [x] Presión de llantas: para RENTA y OTRO ahora se pueden agregar/quitar filas de llanta ("+ Agregar llanta", mínimo 1) ya que esos viajes no tienen un camión de catálogo que fije la cantidad real de llantas — confirmado funcionando
- [x] Pantalla 1: navegación automática entre campos — al seleccionar Nombre se abre solo Tipo de Unidad; al elegir tipo se abre/enfoca el siguiente campo según la rama (Detalle Renta→Placas para RENTA, Marca→Placas para OTRO, Placas para GDE/MED); al terminar Placas salta el foco a Económico — confirmado funcionando

### Sesión tarde/noche (2026-08-23): "Otro" en dropdowns, compresión de fotos, tema de color
- [x] Placas GDE/MED: dropdown + opción "Otro" (texto manual)
- [x] Placa RENTA: dropdown + opción "Otro" (texto manual)
- [x] Detalle Renta (marca): dropdown + opción "Otro" (texto manual)
- [x] Chofer: se mantiene solo catálogo (decisión explícita del usuario, no cambia)
- [x] Fix de bug preexistente: `detalleRenta` se validaba como obligatorio pero nunca se guardaba en Firestore — ahora sí se persiste
- [x] Fotos: redimensión (máx. 1280px) + recompresión JPEG (calidad 75%) antes de subir a Storage, corrige rotación EXIF
- [x] Tema de color con verde de marca Panissimo (`#1F6E44`): TopAppBar, botones, títulos de sección del checklist; desactivado dynamic color de Android 12+
- [x] Repo actualizado en GitHub + Release v0.2-demo con el APK más reciente

### Debate: stack de la App Supervisor
- Usuario pidió cambiar de plan original (Java/Swing) a **app web**
- Propuesta acordada (NO iniciada): React + Vite + Firebase Web SDK cliente + Firebase Hosting (ver CLAUDE.md → "Decisiones de Diseño — App Supervisor")
- Se resolvió también: Firebase Storage requiere plan Blaze (ya activado); costo esperado real $0 para esta escala de uso
- Usuario dio especificación detallada de requisitos del supervisor (filtros, CRUD con soft-delete, imprimir, rendimiento combustible) — documentada en CLAUDE.md
- **Próximo paso cuando se retome**: confirmar arranque del proyecto web (estructura Vite, conexión a Firebase, primera pantalla de catálogos)

---

## 📊 Overview por Componente

| Componente | Estado | % Completo |
|-----------|--------|-----------|
| **Android App** | ✅ Flujo completo (chofer → checklist → bitácora → fotos → tema de color) | 100% |
| **Firebase Backend** | ✅ Listo (Firestore + Storage con plan Blaze) | 100% |
| **App Supervisor (Web)** | ✅ Todo lo especificado en CLAUDE.md implementado y desplegado en https://checklist-choferes.web.app (Dashboard con métricas, Detalle de viaje, Imprimir, CRUD catálogos, tema de marca) | 100% |
| **Fotos/Storage** | ✅ Completo (con compresión) | 100% |
| **Botones especiales** | ✅ Listo | 100% |
| **Distribución** | ✅ Repo privado en GitHub (`ivssun/checklist-chofer`) + APK debug en Release (v0.4-demo, la más reciente) | 100% |

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

## 🔶 En Progreso

### App Supervisor (Web — React + Vite + Firebase Hosting) — carpeta `supervisor-web/`
**Cambio de plan (2026-08-23)**: reemplaza el plan original de Java + Swing + Apache POI. Ver "Decisiones de Diseño — App Supervisor" en CLAUDE.md para el debate completo. Arrancado 2026-09-01, ver sesión correspondiente arriba para el detalle.
- [x] Proyecto Vite inicial + conexión a Firebase Web SDK (Firestore + Storage)
- [x] Dashboard principal v1 (`src/pages/Dashboard.jsx`)
- [x] **Filtros**: fecha (desde/hasta), placa, chofer, destino
- [x] **CRUD Catálogos** (`src/pages/Catalogos.jsx`, ruta `/catalogos`, botón "Administrar catálogos" desde el Dashboard) — confirmado funcionando por el usuario (2026-09-02)
  - [x] Agregar/editar/eliminar (soft) choferes
  - [x] Agregar/editar/eliminar (soft) camiones (tipo, placa, no. unidad, km último servicio, posiciones de llantas dinámicas — default Delantera Izq/Der, Trasera Izq/Der)
  - [x] Agregar/editar/eliminar (soft) destinos
  - [x] Soft-delete (bool activo → inactivo) en todos los catálogos, con botón "Reactivar" para revertir
- [x] **Visualización de viajes**:
  - [x] Filtros aplicados
  - [x] Estado: activo vs concluido (badge)
  - [x] Datos en tabla
- [x] **Detalle de viaje** (`src/pages/ViajeDetalle.jsx`, ruta `/viajes/:viajeId`, click desde fila del Dashboard) — confirmado funcionando por el usuario (2026-09-02)
  - [x] Todos los campos del checklist, itinerario, cargas de combustible, notas/fotos
  - [x] Rendimiento combustible: (KM_final_último - KM_inicial_primero) / sum(litros), solo válido si tanque salió y regresó lleno
  - [x] Alerta servicio: si (km_salida - kilometrajeUltimoServicio) ≥ 9000
  - Nota: al construir esta pantalla se detectó que CLAUDE.md documentaba nombres de campo de `inspeccionGeneral`/`documentacionEquipo` que nunca existieron en `Models.kt` (p. ej. `nivelAceiteMotor` vs el real `nivelAceite`, `copiaSUA`/`polizaSeguro`/`equipoSeguridadCompleto` vs los reales `licenciaChofer`/`segurosVehiculo`/`documentoViaje`) — corregido en CLAUDE.md
- [x] **Imprimir formato con respuestas** — confirmado funcionando por el usuario (2026-09-02)
  - [x] Vista HTML imprimible (`src/pages/ImpresionChecklist.jsx`), replica el formato Word real de la empresa (logo Panissimo, encabezados azul marino, checkboxes SÍ/NO/BIEN/MAL/N/A, firmas al pie) — plantilla de referencia guardada en `supervisor-web/reference/Checklist.pdf`
  - [x] "Imprimir → Guardar como PDF" del navegador, con nombre de archivo sugerido `Checklist_{viajeId}` (vía `document.title` antes de `window.print()`)
  - [x] Filas dinámicas (solo destinos/cargas reales, sin límite fijo de 3/6 como el papel)
  - Nota: la carpeta de destino del PDF no se puede forzar desde la web (limitación de navegador, no de la app) — el usuario la configura una vez en los ajustes de descargas de su navegador
- [x] **Problemas pendientes**: alerta en el Dashboard con lista de incidentes (`estado: "Pendiente"`)
  - [x] Botón "Marcar como resuelto" — confirmado funcionando por el usuario (2026-09-02)
- [x] **Tema de marca**: paleta Mantine con verde Panissimo (`#1F6E44`) como `primaryColor`, header con logo en todas las pantallas, encabezados de sección resaltados (Detalle de viaje) — confirmado funcionando por el usuario (2026-09-02)
- [x] **Deploy**: Firebase Hosting, link público https://checklist-choferes.web.app — redesplegado 2026-09-02 con CRUD de catálogos, tema de marca, y métricas del Dashboard
- [x] **Dashboard — datos útiles de la spec original**: columnas de combustible cargado, rendimiento y badge ⚠️ de alerta de servicio (≥9,000 km) por viaje, calculadas leyendo `destinos`/`cargasCombustible` de cada uno — confirmado funcionando por el usuario (2026-09-02)

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
7. ❌ App Supervisor (web, ver arriba) — no iniciada
8. ❌ Impresión de reporte (vista imprimible del navegador, no Word)

**Plan**: Android funcional completo, incluyendo fotos. App Supervisor = fase 2.

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

