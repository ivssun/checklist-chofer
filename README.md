# Checklist Digital de Flotilla

Prototipo funcional para digitalizar el checklist diario de seguridad que llenan los choferes de una flotilla de camiones (hoy en papel, se pierden hojas). Es una demo para presentar a un cliente informal — prioridad: velocidad de desarrollo sobre estética o arquitectura perfecta.

## Stack

- **App móvil (chofer)**: Android nativo, Kotlin, Jetpack Compose, minSdk 26.
- **Backend**: Firebase Firestore (proyecto `checklist-choferes`, modo prueba — sin reglas de seguridad todavía) + Firebase Storage (fotos).
- **App Supervisor (web)**: aún no iniciada. Planeada como React + Vite + Firebase Web SDK + Firebase Hosting.

## Estado actual

- ✅ App Android: flujo completo (registro de viaje → checklist de 18 puntos de inspección → itinerario/bitácora por destino → cargas de combustible → fotos).
- ❌ App Supervisor (web): no iniciada.

Para el detalle de qué está hecho, pendiente, y el historial de decisiones de producto, ver [`PROGRESS.md`](./PROGRESS.md) y [`CLAUDE.md`](./CLAUDE.md) (este último documenta el porqué de cada decisión de negocio/UX, pensado originalmente como contexto para asistencia con IA, pero sirve como bitácora técnica).

## Cómo correr el proyecto

1. Abrir la carpeta en Android Studio.
2. Conectar un dispositivo o iniciar un emulador (Android 8.0 / API 26 en adelante).
3. Correr la app (`Run` ▶ o `./gradlew installDebug` desde terminal).
4. Al primer arranque, la app crea automáticamente catálogos de prueba en Firestore (choferes, camiones, destinos) si están vacíos — no hace falta cargar nada a mano para probar.

No requiere login: el acceso a Firestore está abierto (modo prueba) mientras el proyecto es un prototipo.

## Flujo de la app (chofer)

1. **Pantalla 1** — registro del viaje: chofer, tipo de unidad (ISUZU GDE/MED/RENTA u OTRO), placas, número económico.
2. **Checklist** — combustible/limpieza, inspección general (18 puntos, incluye presión de llantas y nivel de urea), documentación y equipo, observaciones. Fotos opcionales por cámara (nunca galería).
3. **Control de viaje** — itinerario por destino (iniciar/llegar con kilometraje y canastillas), cargas de combustible en cualquier momento.

Si la app se cierra a medio viaje, al reabrir pregunta si quieres continuar ese viaje o cancelarlo e iniciar uno nuevo (útil si te cambiaron de unidad o cancelaron el viaje).

## Limitaciones conocidas (por ser prototipo)

- Sin autenticación — acceso directo.
- El checklist (secciones C-F) no se autoguarda campo por campo: si la app se cierra a media sección sin presionar "Guardar", se pierde lo tecleado en esa sesión.
- Reglas de Firestore abiertas (modo prueba) — no usar con datos sensibles reales todavía.
- Los catálogos (choferes, camiones, destinos) solo se administran hoy editando Firestore directamente o vía el catálogo semilla en el código — la app Supervisor (cuando exista) será la forma pensada para administrarlos sin tocar la base de datos.

## Contribuir

Antes de tocar código, revisa `PROGRESS.md` para el estado más reciente. Si cambias una decisión de negocio o UX, actualiza `CLAUDE.md` para que quede documentado el porqué.
