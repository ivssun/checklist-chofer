# CLAUDE.md — Proyecto Checklist Digital de Flotilla

## Contexto general
App para digitalizar el checklist diario de seguridad de choferes de una flotilla de camiones (actualmente en papel, se pierden hojas). Demo para presentar a un cliente informal, prioridad: velocidad de desarrollo sobre estética. NO es una entrega de trabajo formal, es un prototipo funcional.

## Preferencias de trabajo del usuario
- Es estudiante, pero para este proyecto quiere AVANZAR RÁPIDO, no explicaciones detalladas de código línea por línea.
- SÍ requiere verificación de que las cosas funcionan: después de completar cada sección del formulario (ver checklist en PROGRESO.md), compilar, correr en emulador/dispositivo, y confirmar que la función trabaja y que los datos se guardan correctamente en Firestore antes de avanzar a la siguiente sección. Pausar y pedir confirmación del usuario en cada uno de estos checkpoints.
- No se requiere UI bonita, solo funcional.

## Stack técnico
- **App móvil**: Android nativo, Kotlin, Jetpack Compose, minSdk 26 (Oreo)
- **App supervisor**: cambiada de plan (2026-08-23) — ver [Decisiones de Diseño — App Supervisor](#-decisiones-de-diseño--app-supervisor-2026-08-23) más abajo. Propuesta actual (NO iniciada aún, pendiente de confirmación final para arrancar): **app web** (React + Vite) + Firebase Web SDK cliente (sin backend propio) + **Firebase Hosting** para publicarla con un link. Reemplaza el plan original de Java/Swing/Apache POI.
- **Backend**: Firebase Firestore (proyecto checklist-choferes, modo prueba, región us-central1) + Firebase Storage (para fotos, plan Blaze activo desde 2026-08-23 — ver nota de facturación abajo)
- Sin restricciones de librerías adicionales si son necesarias

## Esquema de datos (Firestore)

### Colección choferes (catálogo)
{ nombre: string, activo: bool }

### Colección camiones (catálogo)
{
  id: string,
  placa: string,
  economico: string,            // No. de Unidad / Económico (ej. "ECO-001")
  tipo: "GDE" | "MED" | "RENTA",
  kilometrajeUltimoServicio: number,  // Alerta si kmInicial - this >= 9000
  posicionesLlantas: string[],  // ej. ["Delantera Izq", "Delantera Der", ...]
  activo: bool
}

### Colección destinosCatalogo (catálogo)
{ nombre: string, activo: bool }

### Colección viajes (documento principal por checklist)
{
  id: string,
  choferId: string,
  camionId: string|null,      // null si tipo = RENTA u Otro
  tipoUnidad: "GDE" | "MED" | "RENTA" | "Otro",
  placa: string,              // placa del vehículo (ej. "ABC123", "RENTA001")
  detalleRenta: string,       // marca/modelo, solo aplica si tipoUnidad = RENTA (string vacío si no aplica)
  economico: string,          // No. de Unidad / Económico (ingreso manual, obligatorio)
  fecha: timestamp,           // automática al crear
  horaLlegadaMatriz: timestamp|null,  // se asigna al crear
  concluido: bool,            // false al crear, true al completar último destino

  combustibleYLimpieza: {
    tanqueLlenoSalida: { valor: bool, observacion: string, fotoURL: string|null },
    tanqueLlenoRegreso: { valor: bool, observacion: string, fotoURL: string|null },
    limpiezaCajaCabina: { valor: bool, observacion: string, fotoURL: string|null }
  },

  inspeccionGeneral: {
    llantasDesgaste, llantaRefaccion, sistemaFrenado, luces, espejos,
    limpiaparabrisasClaxon, nivelAceiteMotor, nivelAgua, nivelLiquidoFrenos,
    bateria, triangulosSeguridad, gatoHidraulico, carroceria,
    candadosCaja, bandasSeguridadCaja
    // cada uno: { valor: "bien"|"mal"|"na", observacion: string, fotoURL: string|null }
  },

  presionLlantas: [ { etiqueta: string, presion: number } ],
  presionLlantasObservacion: { observacion: string, fotoURL: string|null },
  ureaPorcentaje: number,       // 0-100
  ureaObservacion: { observacion: string, fotoURL: string|null },
  combustibleThermo: "0" | "1/4" | "1/2" | "3/4" | "Lleno",
  combustibleThermoObservacion: { observacion: string, fotoURL: string|null },

  documentacionEquipo: {
    copiaSUA, polizaSeguro, tarjetaCirculacion, equipoSeguridadCompleto
    // cada uno: { valor: bool, observacion: string, fotoURL: string|null }
  },

  observacionesGenerales: string
}

### Subcolección viajes/{viajeId}/destinos
{
  orden: number,               // define secuencia del itinerario
  cedisDestino: string,
  kmInicial: number|null,      // se llena al "Iniciar viaje a X"
  kmFinal: number|null,        // se llena al "Ya llegué a X"
  fechaSalida: timestamp|null,
  fechaLlegada: timestamp|null,
  canastillasEntregadas: number|null,
  canastillasRegresadas: number|null,
  nota: string|null,           // opcional, solo al llegar
  fotoURL: string|null         // opcional, solo cámara, NO se imprime
}

### Subcolección viajes/{viajeId}/cargasCombustible
{
  ubicacion: string,
  kilometraje: number,
  costoPorLitro: number,
  litros: number,
  fechaCarga: timestamp        // automática al agregar
}

## FLUJO APP ANDROID (Chofer)

### Pantalla 1: Formulario inicial (ViajeScreen) — IMPLEMENTADO ✅

**Campos mostrados**:
1. **Fecha**: automática, no editable (DD/MM/YYYY)
2. **Nombre**: dropdown de choferes activos (autocomplete)
3. **Tipo de Unidad**: dropdown (GDE, MED, RENTA, Otro)
4. **Placas** (dinámico según tipo):
   - **GDE/MED**: dropdown de camiones filtrados por tipo (muestra placa) + opción "Otro" (texto manual, por si el camión no está registrado en el catálogo)
   - **RENTA**: dropdown de placas predefinidas (RENTA001 a RENTA006) + opción "Otro" (texto manual)
   - **Otro** (tipo de unidad): campo de texto manual directo (ingresa placa)
5. **Detalle Renta** (solo si tipo = RENTA): dropdown de marcas (Ford, Toyota, Caja, Redila, Batea, Volteo) + opción "Otro" (texto manual)
6. **No. de Unidad / Económico**: campo de texto (ingreso manual, obligatorio para todos)

**Nota sobre "Otro" en dropdowns de catálogo**: por decisión del usuario (2026-08-23), los dropdowns de Placas (GDE/MED y RENTA) y Detalle Renta incluyen una opción "Otro" que revela un campo de texto libre, para no bloquear al chofer si el dato no está registrado en el catálogo. El dropdown de **Chofer NO tiene esta opción** — el roster de choferes se mantiene siempre controlado por catálogo (se administra desde la app de supervisor).

**Validaciones**:
- Chofer requerido
- Económico requerido (nunca vacío)
- Tipo GDE/MED: requiere camión seleccionado
- Tipo RENTA: requiere placa RENTA + detalle marca
- Tipo Otro: requiere placa manual

**Guardado en Firestore**:
- `choferId`: ID del chofer seleccionado
- `tipoUnidad`: tipo seleccionado
- `placa`: placa del vehículo
- `economico`: valor ingresado manualmente
- `camionId`: ID del camión (null si RENTA u Otro)
- `fecha`: timestamp automático
- `concluido`: false al crear

**Sección B — Itinerario del viaje**
- Botón "+ Agregar destino" → lista de destinosCatalogo activos
- Lista visual ordenada por orden de agregado: "1. UV", "2. Plaza Américas"... con botón "x" para eliminar
- Mínimo 1 destino requerido
- Se congela (no editable) una vez guardado el checklist

**Sección C — Combustible y limpieza**
- 3 preguntas sí/no + observación + foto opcional (cámara únicamente)

**Sección D — Inspección general**
- 15 preguntas bien/mal/n-a + observación + foto opcional
- Presión de llantas: tabla dinámica según posicionesLlantas del camión (o 6 genéricas editables si "Otro") + observación/foto opcional propia
- Urea: numérico 0-100 (porcentaje) + observación/foto opcional propia
- Combustible Thermo: selector único (0, 1/4, 1/2, 3/4, Lleno) + observación/foto opcional propia
- Las 18 preguntas de esta sección (incluye presión de llantas, urea y thermo) tienen observación y foto opcional

**Sección E — Documentación y equipo**
- 4 preguntas sí/no + observación + foto opcional

**Sección F — Observaciones generales**
- Texto libre

**Botón "Guardar"**
- Habilitado solo si: chofer + tipo/camión + al menos 1 destino + TODAS las preguntas de C, D, E respondidas (incluye llantas, urea, thermo)
- Al guardar: crea documento viajes (concluido: false), sube fotos a Storage, crea subdocumentos destinos (con orden y cedisDestino, resto null)
- Navega a Pantalla 2

### Pantalla 2: Control de viaje
- Muestra itinerario congelado
- Botón dinámico según estado del destino pendiente:
  - Sin fechaSalida → "Iniciar viaje a [destino]" → pide km inicial, guarda fechaSalida (auto) + kmInicial
  - Con fechaSalida sin fechaLlegada → "Ya llegué a [destino]" → pide km final, canastillas entregadas/regresadas, nota+foto opcionales → guarda fechaLlegada (auto)
  - Al completar último destino → viaje.concluido = true
- Botón siempre visible: "+ Agregar carga de combustible" (ubicación, kilometraje, costo/litro, litros) → cualquier momento, no bloquea nada

## FLUJO APP SUPERVISOR — PENDIENTE DE INICIAR (propuesta, no confirmada aún)

Acceso: desde Windows, vía navegador (URL de Firebase Hosting), no una app instalada. Ver debate de stack en [Decisiones de Diseño — App Supervisor](#-decisiones-de-diseño--app-supervisor-2026-08-23).

Hay 2 tipos de usuario en el sistema: **Chofer** (Android) y **Supervisor** (esta app web). Sin autenticación/login por ahora (acceso directo, ver "Fuera de alcance").

### Dashboard
- Filtros: fecha, placa, chofer, destino
- Lista de viajes con indicador **activo vs concluido** (campo `concluido` bool, ya existe en Firestore)
- Alerta visual si diferencia km >= 9000 vs kilometrajeUltimoServicio
- Datos útiles a mostrar: cantidad de combustible cargado por viaje, rendimiento (ver fórmula abajo), y otras métricas que se consideren útiles

### Detalle de viaje (solo lectura, sin edición)
- Todos los campos del checklist, itinerario, notas/fotos
- Botón "Imprimir" con las respuestas ya asentadas — propuesta: vista HTML imprimible + diálogo nativo del navegador "Imprimir → Guardar como PDF" (reemplaza el plan original de generar .docx con Apache POI; más simple y rápido de construir)
- Rendimiento combustible: (kmFinal último destino − kmInicial primer destino) / suma litros cargados durante el viaje. **Nota**: solo válido si el tanque salió y regresó lleno al 100%

### Administración de catálogos (CRUD, soft-delete con bool activo)
- Pestañas: Choferes / Camiones (Placas) / Destinos
- El supervisor no sabe de bases de datos — se facilita con botones explícitos tipo "Agregar empleado", "Editar empleado", "Eliminar empleado", "Agregar placas", etc. (uno por tipo de catálogo)
- Eliminar = soft-delete (cambiar bool `activo` a `false`/apagado, nunca se borra el registro)
- Alta de camión incluye definir `posicionesLlantas` manualmente
- Esta administración es también el lugar para dar de alta un chofer/placa que un chofer haya tenido que escribir "a mano" en Android por no estar aún en el catálogo (ver opción "Otro" en Pantalla 1 de Android)

## Reglas de negocio críticas
1. Alerta servicio: kmInicial primer destino − kilometrajeUltimoServicio >= 9000 -> aviso en escritorio
2. Rendimiento combustible: ver fórmula arriba, condicionado a tanque lleno
3. Presión de llantas: filas = tamaño de posicionesLlantas del camión, o 6 editables si "Otro"
4. Itinerario se congela tras guardar checklist principal
5. Fotos: SOLO cámara en vivo, nunca galería (Intent de cámara directo)
6. Firmas: NO se capturan digitalmente, se firman a mano sobre documento impreso

## Fuera de alcance para esta demo
- Autenticación/login (acceso directo por ahora)
- Reordenar destinos ya agregados (se resuelve eliminando y re-agregando)
- Modificar itinerario a mitad de viaje

## 📝 Decisiones de Diseño — Pantalla 1 (2026-08-23)

### RENTA: Estructura de 3 campos
Inicialmente se pensó en mostrar solo "marcas" para RENTA. Pero el usuario aclaró que:
- **Placas**: dropdown predefinido (RENTA001-RENTA006) = identificador técnico del vehículo
- **Detalle Renta**: dropdown de marcas (Ford, Toyota, etc.) = tipo de vehículo
- **Económico**: ingreso manual = identificador interno/asignación del usuario

**Razón**: Separación entre "qué vehículo es" (técnico) y "quién es para nosotros" (negocio).

### No. de Unidad / Económico: Ingreso manual para TODOS
- Originalmente: se pensaba mostrarlo automático (del catálogo)
- Ahora: ingreso manual obligatorio en todos los casos
- **Razón**: flexibilidad. El usuario asigna un "económico" específico para ese viaje/rol

### Scroll vertical
- Necesario porque ViajeScreen tiene muchos campos
- Implementado: `Column(...).verticalScroll(rememberScrollState())`

### UI Polish Pendiente (sin placeholders, bordes uniformes)
- Placeholders: están en el código como removidos pero siguen viéndose (revisar compilación/caché)
- Bordes redondeados: RoundedCornerShape(8.dp) agregado pero no se ve la diferencia con OutlinedButton

## 📝 Decisiones de Diseño — App Supervisor (2026-08-23)

### Cambio de plan: Java/Swing (escritorio) → Web (navegador)
El plan original de este archivo era una app de escritorio en Java + Swing + Apache POI + Firebase Admin SDK. El usuario pidió cambiarlo a una **app web** ("accede desde Windows" = navegador en una PC Windows, no un ejecutable instalado).

**Stack propuesto** (recomendado por Claude, aceptado por el usuario, pero la construcción del proyecto AÚN NO ha arrancado — se pausó para seguir ajustando la app Android):
- **React + Vite**: frontend, sin backend propio
- **Firebase Web SDK** (Firestore + Storage) directo desde el cliente — las reglas de Firestore ya están en modo prueba (abiertas), igual que hace la app Android, así que no hace falta montar un servidor con Admin SDK
- **Firebase Hosting** para publicar: da una URL estable tipo `https://checklist-choferes.web.app`, deploy con un comando, capa gratuita de sobra para esta demo (10GB almacenamiento, 360MB/día transferencia)
- **IDE recomendado**: VS Code (ya lo tiene instalado el usuario) en vez de Android Studio/IntelliJ, por ser el estándar para proyectos web
- **"Imprimir"**: en vez de generar `.docx` con Apache POI, usar una vista HTML imprimible + "Imprimir → Guardar como PDF" del navegador — mucho más simple

**Razón del cambio**: prioridad de velocidad de desarrollo (ver Contexto general). Evita Java Swing (UI anticuada, más lenta de construir) y evita montar un backend/servidor separado solo para usar el Admin SDK, cuando el cliente web puede hablar directo a Firestore igual que la app Android.

### Nota de facturación: Firebase Storage requiere plan Blaze
Al intentar habilitar Firebase Storage (2026-08-23) para las fotos de Android, la consola pidió activar el plan **Blaze** (pago por uso) — Storage ya no está disponible en el plan gratuito Spark para proyectos nuevos. El usuario activó Blaze (vinculó cuenta de facturación de Google Cloud). Esto **ya está resuelto y activo**, relevante aquí porque la app del supervisor usará el mismo proyecto de Firebase:
- Blaze no cobra nada extra por sí solo; solo se paga si se excede la capa gratuita (Firestore: 1GB/50K lecturas-día/20K escrituras-día; Storage: 5GB/1GB descarga-día; Hosting: 10GB/360MB-día)
- Para esta demo/prototipo el uso real esperado es $0
- Recomendación pendiente de aplicar: comprimir fotos antes de subir (✅ ya implementado en Android, ver PROGRESS.md) y configurar una alerta de presupuesto en Firebase Console (Facturación → Presupuestos y alertas) como red de seguridad

### Especificación detallada dada por el usuario (2026-08-23) para la app Supervisor
Instrucciones textuales del usuario, para referencia al implementar:
- Todo lo que sea una lista debe tener una tabla en la BD editable por el supervisor, facilitado con botones tipo "Agregar empleado" / "Editar empleado" / "Eliminar empleado" / "Agregar placas" (el supervisor no sabe de bases de datos)
- Eliminaciones = soft-delete (bool activo/inactivo, nunca se borra)
- Los datos que se muestran como listas de opciones al chofer (placas, etc.) deben tener opción "Otro" con texto libre, para no bloquear al chofer si algo no está registrado (✅ ya implementado en Android para Placas GDE/MED, Placa RENTA y Detalle Renta — Chofer se mantiene solo-catálogo por decisión explícita)
- 2 tipos de usuario: Chofer (Android) y Supervisor (Windows/navegador)
- Supervisor: filtros por fecha/placa/chofer/destino; agregar/eliminar(soft) catálogos; diferenciar rutas activas vs concluidas (bool); imprimir formato con respuestas; ver cantidad de combustible cargado, rendimiento (fórmula arriba, solo válido con tanque lleno ida y vuelta), y otras métricas útiles a criterio de Claude

## Instrucciones para Claude Code
- Antes de generar código nuevo, revisa PROGRESS.md para saber el estado actual.
- Al completar cada sección (Pantalla 1, Pantalla 2, etc.), DETENTE, compila, corre en emulador, y pide al usuario que confirme que funciona antes de continuar.
- Actualiza PROGRESS.md marcando lo completado después de cada checkpoint confirmado.
- Si el usuario aclara o cambia una decisión de negocio, actualiza este archivo (CLAUDE.md) para reflejarlo.
- No expliques el código en detalle; enfócate en avanzar rápido pero con verificaciones funcionales reales.
