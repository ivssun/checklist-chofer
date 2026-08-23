# CLAUDE.md — Proyecto Checklist Digital de Flotilla

## Contexto general
App para digitalizar el checklist diario de seguridad de choferes de una flotilla de camiones (actualmente en papel, se pierden hojas). Demo para presentar a un cliente informal, prioridad: velocidad de desarrollo sobre estética. NO es una entrega de trabajo formal, es un prototipo funcional.

## Preferencias de trabajo del usuario
- Es estudiante, pero para este proyecto quiere AVANZAR RÁPIDO, no explicaciones detalladas de código línea por línea.
- SÍ requiere verificación de que las cosas funcionan: después de completar cada sección del formulario (ver checklist en PROGRESO.md), compilar, correr en emulador/dispositivo, y confirmar que la función trabaja y que los datos se guardan correctamente en Firestore antes de avanzar a la siguiente sección. Pausar y pedir confirmación del usuario en cada uno de estos checkpoints.
- No se requiere UI bonita, solo funcional.

## Stack técnico
- **App móvil**: Android nativo, Kotlin, API mínima 8 (Oreo)
- **App escritorio**: Java, Swing (UI), Apache POI (generación de Word), Firebase Admin SDK
- **Backend**: Firebase Firestore (proyecto checklist-choferes, ya creado, modo prueba, región us-central1) + Firebase Storage (para fotos)
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
   - **GDE/MED**: dropdown de camiones filtrados por tipo (muestra placa)
   - **RENTA**: dropdown de placas predefinidas (RENTA001 a RENTA006)
   - **Otro**: campo de texto manual (ingresa placa)
5. **Detalle Renta** (solo si tipo = RENTA): dropdown de marcas (Ford, Toyota, Caja, Redila, Batea, Volteo)
6. **No. de Unidad / Económico**: campo de texto (ingreso manual, obligatorio para todos)

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

## FLUJO APP ESCRITORIO (Supervisor, Windows/Java) — fase posterior a Android

### Dashboard
- Filtros: fecha, placa, chofer, destino
- Lista de viajes con indicador activo/concluido
- Alerta visual si diferencia km >= 9000 vs kilometrajeUltimoServicio

### Detalle de viaje (solo lectura, sin edición)
- Todos los campos del checklist, itinerario, notas/fotos
- Botón "Imprimir/Generar Word" (Apache POI, usa plantilla de Checklist.docx)
- Rendimiento combustible: (kmFinal último destino − kmInicial primer destino) / suma litros cargados. Nota: solo válido si tanque salió y regresó lleno al 100%

### Administración de catálogos (CRUD, soft-delete con bool activo)
- Pestañas: Choferes / Camiones / Destinos
- Botones Agregar/Editar/Eliminar (eliminar = activo -> false, nunca borra)
- Alta de camión incluye definir posicionesLlantas manualmente

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

## Instrucciones para Claude Code
- Antes de generar código nuevo, revisa PROGRESS.md para saber el estado actual.
- Al completar cada sección (Pantalla 1, Pantalla 2, etc.), DETENTE, compila, corre en emulador, y pide al usuario que confirme que funciona antes de continuar.
- Actualiza PROGRESS.md marcando lo completado después de cada checkpoint confirmado.
- Si el usuario aclara o cambia una decisión de negocio, actualiza este archivo (CLAUDE.md) para reflejarlo.
- No expliques el código en detalle; enfócate en avanzar rápido pero con verificaciones funcionales reales.
