# ChecklistChofer - Progress & Status

**Última actualización**: 2026-09-05  
**Demo objetivo**: Lunes 2026-08-25 (ya pasada) — cliente real dando feedback activo, iterando día a día  
**Estado general**: Android y Supervisor web completos, con dos rondas extra de feedback del cliente real (2026-09-04 y 2026-09-05) ya implementadas y confirmadas: reestructuración del checklist por secciones, validaciones de kilometraje y canastillas, edición de itinerario antes de iniciar, estado "Cancelado", impresión en horizontal con mejor espaciado, filtros nuevos y datos en vivo (`onSnapshot`) en el Dashboard, sección de incidentes visible en Detalle de viaje e impresión, e ícono de ayuda para el cálculo de rendimiento. Todo confirmado funcionando por el usuario y en vivo en https://checklist-choferes.web.app. Ver "Sesión 2026-09-04/05" más abajo para el detalle completo.

### Sesión 2026-09-04/05: Segunda ronda de feedback del cliente real (Android + Supervisor web)
Todo implementado y confirmado funcionando por el usuario en dispositivo/navegador real. Detalle completo de cada punto en las secciones correspondientes más abajo (buscar por título):
- **Android — ChecklistScreen**: pantalla "hub" con una tarjeta por sección (progreso + contador) en vez de scroll único; botón "Continuar llenando checklist" en rojo al final de cada sección; selector de combustible Thermo con botones visibles (sin dropdown); foto opcional en Observaciones Generales; NEXT/DONE encadenado en presión de llantas
- **Android — ControlViajeScreen**: itinerario editable (agregar/quitar destino) mientras el viaje no ha arrancado; advertencia (con opción de confirmar) si el km inicial es menor al del último servicio del camión; bloqueo total si el km inicial es menor al km final del destino anterior, o si el km final es menor al inicial del mismo destino; nueva pregunta "Canastillas iniciales" al iniciar el primer destino; bloqueo si "canastillas entregadas" excede lo disponible
- **Android — estado "Cancelado"**: al elegir "Cancelar e iniciar un nuevo viaje" ahora se marca `cancelado: true` en Firestore (antes quedaba huérfano sin ninguna marca)
- **Supervisor web — Dashboard**: datos en vivo (`onSnapshot`, sin recargar) para viajes e incidentes pendientes; filtro de Estado (Activo/Concluido/Cancelado); switch "Solo unidades con alerta de servicio"; ícono de ayuda "?" junto a Rendimiento con la fórmula
- **Supervisor web — Detalle de viaje**: también en vivo (viaje, destinos, cargas); nuevos campos "Km último servicio" y "Canastillas iniciales"; alerta roja si el km inicial es menor al del último servicio; sección "Incidentes reportados" (visible aunque estén Resueltos, a diferencia del Dashboard)
- **Supervisor web — Impresión**: horizontal en vez de vertical; filas con observación (páginas 1 y 3) con el doble de espacio; más espacio antes de firmas (duplicado dos veces: 16px → 32px → 64px); "Observaciones generales" con el triple de espacio; nuevo campo "Canastillas iniciales"; nueva sección "Incidentes reportados"
- **Datos de prueba**: se borraron `viajes`/`incidentes` (acumulaban basura de todo un día de pruebas) y se repobló con 6 viajes + 3 incidentes curados a mano que cubren todos los estados y features nuevas (ver sección "Reset de datos de prueba" más abajo)

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
| **Android App** | ✅ Flujo completo (chofer → checklist → bitácora → fotos → tema de color) + 2ª ronda de feedback del cliente real (2026-09-04/05) | 100% |
| **Firebase Backend** | ✅ Listo (Firestore + Storage con plan Blaze) | 100% |
| **App Supervisor (Web)** | ✅ Todo lo especificado en CLAUDE.md implementado y desplegado en https://checklist-choferes.web.app (Dashboard con métricas y datos en vivo, Detalle de viaje, Imprimir en horizontal, CRUD catálogos, tema de marca) | 100% |
| **Fotos/Storage** | ✅ Completo (con compresión) | 100% |
| **Botones especiales** | ✅ Listo | 100% |
| **Distribución** | ✅ Repo privado en GitHub (`ivssun/checklist-chofer`) + APK debug en Release (v0.5-demo, la más reciente) | 100% |

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
- [x] Combustible Thermo en posición 12 (botones visibles, sin dropdown — cambiado 2026-09-04)
- [x] Itinerario: agregar/eliminar destinos (bloquea post-guardado)
- [x] Botones SÍ/NO/BIEN/MAL/N/A destacados visualmente
- [x] Sin números en etiquetas de preguntas
- [x] Sin "18 campos" en título

### UI - ChecklistScreen: reestructuración por secciones (2026-09-04, feedback cliente real) — confirmado funcionando en dispositivo
- [x] Pantalla "hub" con tarjeta por sección (Itinerario, Combustible y Limpieza, Inspección General, Documentación y Equipo, Observaciones Generales), cada una con contador "X de Y puntos" + anillo de progreso, navega a pantalla propia de esa sección al tocarla
- [x] Botón "Guardar Checklist Completo" se queda en el hub (no dentro de cada sección)
- [x] Combustible Thermo: opciones visibles como botones en vez de dropdown "Seleccionar"
- [x] Observaciones Generales: agregado botón de cámara (nuevo campo `observacionesGeneralesFotoURL`), para reportar con foto algo extraordinario no cubierto por el formulario
- [x] Supervisor web (`ViajeDetalle.jsx`): muestra link "Ver foto" de `observacionesGeneralesFotoURL` si existe, mismo patrón que el resto de campos con foto
- [x] Presión de llantas: NEXT salta al siguiente campo, DONE en la última llanta
- [x] Botón rojo "Continuar llenando checklist" al final de cada sección (además de la flecha de regreso)

### Android App - ControlViajeScreen: editar itinerario antes de iniciar (2026-09-04, feedback cliente real) — confirmado funcionando en dispositivo
- [x] Botón "✏️ Editar" junto a "Itinerario", visible solo si ningún destino tiene `fechaSalida` (viaje no arrancado) y el viaje no está concluido
- [x] En modo edición: agregar destino (dropdown del catálogo, `agregarDestinoAlItinerario`) y quitar destino (`eliminarDestinoDelItinerario`, mínimo 1 restante)
- [x] Al quitar un destino de en medio, se renumera `orden` de los restantes (contiguo 0..n-1)
- [x] Nuevo método en `FirebaseRepository`: `deleteDestino` (borrado real, no soft-delete — el destino aún no tiene datos capturados en ese punto)

### Advertencia de km inicial sospechoso (2026-09-04, feedback cliente real) — confirmado funcionando en dispositivo
- [x] Android (`DialogoIniciarDestino`): si el km inicial capturado es menor a `kilometrajeUltimoServicio` del camión, se muestra advertencia roja y los botones cambian a "Revisar" / "Sí, confirmar" (no bloquea, solo exige confirmar que no es error de captura)
- [x] `ControlViajeViewModel` ahora también carga el `camion` del viaje (igual que `ChecklistViewModel`) para tener `kilometrajeUltimoServicio` disponible
- [x] Supervisor web (`ViajeDetalle.jsx`): nuevo campo "Km último servicio" en la tarjeta de datos del viaje + alerta roja si el km inicial del primer destino es menor a ese valor — desplegado a https://checklist-choferes.web.app (2026-09-04)

### Validación bloqueante de km final < km inicial (2026-09-05, feedback cliente real) — confirmado funcionando en dispositivo
- [x] `DialogoLlegadaDestino` ("Ya llegué a X"): el botón "Confirmar" ahora se deshabilita si el km final no es mayor al km inicial (antes solo se sabía hasta después de tocar Confirmar, vía snackbar)
- [x] Campo "Km final" marcado en rojo (`isError`) con texto de ayuda inline ("Debe ser mayor al km inicial (X)") mientras el valor no sea válido
- [x] Se conserva la validación en `ControlViajeViewModel.registrarLlegada` como red de seguridad, aunque ya no debería alcanzarse desde la UI

### Dashboard en tiempo real con onSnapshot (2026-09-05, feedback cliente real) — desplegado a https://checklist-choferes.web.app
- [x] `Dashboard.jsx`: colecciones `viajes` e `incidentes` (pendientes) ahora usan `onSnapshot` de Firestore en vez de `getDocs` — la tabla y la alerta de problemas pendientes se actualizan solas si un chofer crea un viaje o reporta un problema desde Android, sin recargar la página
- [x] Catálogos (choferes/camiones/destinosCatalogo) se quedan con carga única (`getDocs`), cambian poco mientras se ve el Dashboard
- [x] Probado con servidor local (`npm run dev`) contra el mismo proyecto Firebase — sin errores de consola, datos cargan correctamente
- [x] `ViajeDetalle.jsx`: el doc del viaje, su itinerario (`destinos`) y sus `cargasCombustible` también se escuchan con `onSnapshot` — si el chofer concluye el viaje, llega a un destino o agrega una carga desde Android mientras el supervisor tiene esa pantalla abierta, se actualiza sola (badge ACTIVO/CONCLUIDO, tabla de itinerario, rendimiento). Chofer/camión/catálogo de destinos se quedan como carga única (no cambian por el progreso del viaje)

### Estado "Cancelado" para viajes abandonados (2026-09-05, feedback cliente real) — confirmado funcionando en dispositivo
- [x] Nuevo campo `Viaje.cancelado: Boolean` (default false)
- [x] Nuevo método `FirebaseRepository.cancelarViaje(viajeId)` — `update("cancelado", true)`
- [x] `RetomarViajeScreen` (pantalla "Viaje en curso"): al elegir "Cancelar e iniciar un nuevo viaje" ahora escribe `cancelado: true` en Firestore antes de navegar a Pantalla 1 (antes el viaje quedaba huérfano sin ninguna marca)
- [x] Supervisor web (Dashboard + `ViajeDetalle.jsx`): badge gris "Cancelado" en vez de "Activo"/"Concluido" cuando corresponde — desplegado a https://checklist-choferes.web.app

### Km inicial no puede bajar del km final del destino anterior (2026-09-05, feedback cliente real) — confirmado funcionando en dispositivo
- [x] `DialogoIniciarDestino` ("Iniciar viaje a X"): si el km inicial es menor al km final del destino anterior del mismo itinerario, el campo se marca en rojo y el botón "Confirmar" se deshabilita — a diferencia de la advertencia de "último servicio" (que sí permite confirmar), este caso se bloquea de plano porque no hay ningún escenario real en el que baje dentro del mismo viaje
- [x] `ControlViajeViewModel.iniciarDestino`: misma validación como red de seguridad server-side

### Pregunta "Canastillas iniciales" (2026-09-05, feedback cliente real) — confirmado funcionando en dispositivo
- [x] Nuevo campo `Viaje.canastillasIniciales: Int?` (con cuántas canastillas sale el camión de la matriz)
- [x] Se pregunta una sola vez, junto con "Km inicial" en el diálogo "Iniciar viaje a X" del **primer** destino del itinerario (no en los siguientes, que ya usan entregadas/regresadas)
- [x] `ControlViajeViewModel.iniciarDestino` guarda el dato en el doc del viaje al confirmar
- [x] Se muestra en `ControlViajeScreen` junto a Placa/Económico una vez capturado
- [x] Supervisor web (`ViajeDetalle.jsx`): campo "Canastillas iniciales" en la tarjeta de datos del viaje, junto a "Km último servicio" — desplegado a https://checklist-choferes.web.app

### Validación de canastillas disponibles (2026-09-05, feedback cliente real) — confirmado funcionando en dispositivo
- [x] `DialogoLlegadaDestino` ("Ya llegué a X"): no se pueden entregar más canastillas de las que trae disponibles el camión en ese punto del viaje (`canastillasIniciales` − entregadas + regresadas de los destinos ya completados antes de este) — campo en rojo + botón "Confirmar" deshabilitado, se bloquea de plano (no hay override, a diferencia de la advertencia de km de servicio)
- [x] `ControlViajeViewModel.registrarLlegada`: misma validación como red de seguridad server-side
- [ ] No se limita "canastillas regresadas" (recoger vacías no tiene un tope natural ligado a `canastillasIniciales`)

### Impresión en horizontal (2026-09-05, feedback cliente real) — desplegado a https://checklist-choferes.web.app
- [x] `index.css`: `@page { size: letter landscape; margin: 12mm; }` (antes `size: letter` = vertical) — todas las tablas de `ImpresionChecklist.jsx` usan `width: 100%` sin anchos fijos, así que aprovechan el ancho extra sin romper el layout
- [x] Confirmado funcionando por el usuario (2026-09-05)

### Ajustes de espaciado en la impresión (2026-09-05, feedback cliente real) — confirmado funcionando por el usuario
- [x] Página 1 (`ImpresionChecklist.jsx`): nuevo renglón "CANASTILLAS INICIALES" en la tabla de datos del viaje
- [x] Filas con columna de observaciones en páginas 1 y 3 (Combustible y limpieza, Documentación y equipo — ambas usan `FilaSiNo`) con el doble de padding vertical (clase `cli-fila-obs-doble`), para que quepa anotar algo a mano. Página 2 (Inspección general, usa `FilaInspeccion`/`FilaLibre`) se queda sin cambios, tal como se pidió
- [x] Más espacio entre las tablas y la sección de firmas (`.cli-pie`, margin-top 16px → 32px) en las 3 páginas
- [x] (2026-09-05, segunda ronda) `.cli-pie` duplicado otra vez (32px → 64px) y `.cli-obs-caja` (Observaciones generales, página 3) triplicado (40px → 120px min-height) — confirmado funcionando por el usuario

### Filtros nuevos en el Dashboard (2026-09-05, feedback cliente real) — probado con servidor local, desplegado a https://checklist-choferes.web.app
- [x] Filtro "Estado" (Activo/Concluido/Cancelado) — se deriva de `viaje.cancelado`/`viaje.concluido`, no es un campo propio
- [x] Switch "Solo unidades con alerta de servicio ⚠️" — filtra por `metricas[v.id]?.alertaServicio` (mismo cálculo ya usado para el badge ⚠️ Servicio de la tabla)

### Reset de datos de prueba (2026-09-05)
Se borraron las colecciones `viajes` (recursivo, con subcolecciones `destinos`/`cargasCombustible`) e `incidentes` — acumulaban muchos registros de pruebas de todo el día. Los catálogos (`choferes`, `camiones`, `destinosCatalogo`) NO se tocaron. Se repobló con 4 viajes de ejemplo curados a mano (script temporal con el SDK cliente de Firebase, reglas en modo prueba, borrado después de correr — no quedó en el repo) que cubren todos los estados y features nuevas del día:
- `demo_viaje_a`: Concluido, buen rendimiento (7.00 km/L), sin alerta de servicio, canastillas iniciales, 2 destinos completos
- `demo_viaje_b`: Concluido, **con alerta de servicio** (km inicial 80,500 vs último servicio 71,000), un punto de inspección en MAL, sin cargas de combustible (rendimiento "—")
- `demo_viaje_c`: **Activo**, primer destino en curso, 2 destinos pendientes
- `demo_viaje_d`: **Cancelado**, tipo Otro (sin camión de catálogo)
- `demo_incidente_1`: Pendiente, ligado a `demo_viaje_b`, con foto (placeholder de placehold.co, no es una foto real de Storage)

### Datos de ejemplo adicionales (2026-09-05) — verificado en el Dashboard desplegado
- `demo_incidente_2` (Pendiente, ligado a `demo_viaje_a`) y `demo_incidente_3` (Pendiente, ligado a `demo_viaje_c`) — para que se vean 2 más en el banner de "problemas pendientes" del Dashboard, además del original
- `demo_viaje_e` (concluido, Maria Torres, GHI789): fotos en "Tanque lleno (salida)" y en "Llantas (desgaste)" (MAL, con foto del desgaste)
- `demo_viaje_f` (concluido, Jose Canela, DEF456): fotos en "Copia de SÚA" (Documentación) y en la observación de Urea
- Mismo método que la primera ronda: script temporal con el SDK cliente de Firebase (reglas en modo prueba), borrado después de correr, no quedó en el repo

### Incidentes visibles en Detalle de viaje e impresión (2026-09-05, feedback cliente real) — desplegado a https://checklist-choferes.web.app
Antes de esto, un incidente resuelto desaparecía de toda la interfaz (el Dashboard solo muestra `estado: "Pendiente"`, y `ViajeDetalle.jsx`/impresión no lo mostraban en absoluto) — el registro seguía en Firestore pero no había forma de volver a verlo.
- [x] `ViajeDetalle.jsx`: nueva sección "Incidentes reportados" (listener `onSnapshot` con `where('viajeId', '==', viajeId)`) — muestra TODOS los incidentes del viaje sin importar su estado, con foto, descripción, fecha, badge Pendiente/Resuelto y fecha de resolución si aplica
- [x] Reporte impreso (`ImpresionChecklist.jsx`, página 3): misma sección como tabla (fecha, descripción, estado), sin foto — consistente con que el resto del documento impreso tampoco imprime fotos de ningún campo
- [x] Probado con servidor local contra el viaje demo con incidente — confirmado renderiza bien en ambos estados (Pendiente y Resuelto)

### Ícono de ayuda "¿Cómo se calcula?" para Rendimiento (2026-09-05, feedback cliente real) — confirmado funcionando por el usuario
- [x] Ícono "?" junto al encabezado "Rendimiento" en la tabla del Dashboard — abre un `Popover` de Mantine (click, no hover) con la fórmula y la condición de tanque lleno ida/vuelta
- [x] La automatización de navegador no pudo confirmarlo visualmente durante el desarrollo (el popover no se mantenía abierto en las capturas — artefacto del click sintético contra la detección de "click afuera" de Mantine/Portal, no del código), pero el usuario lo confirmó funcionando con un clic real

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

