# Checklist Digital de Flotilla

Prototipo funcional para digitalizar el checklist diario de seguridad que llenan los choferes de una flotilla de camiones (hoy en papel, se pierden hojas). Es una demo para un cliente real que ya está dando feedback activo — prioridad: velocidad de desarrollo sobre estética o arquitectura perfecta.

Dos aplicaciones, un mismo backend de Firebase (proyecto `checklist-choferes`):

| | App Chofer (Android) | App Supervisor (Web) |
|---|---|---|
| **Para quién** | El chofer, en su celular | El supervisor, desde su navegador en Windows |
| **Stack** | Kotlin + Jetpack Compose, minSdk 26 | React + Vite (JS, sin TypeScript) + Mantine |
| **Dónde vive en el repo** | Raíz del repo (`app/`, `build.gradle.kts`, etc.) | `supervisor-web/` |
| **Cómo se distribuye** | APK en [GitHub Releases](https://github.com/ivssun/checklist-chofer/releases) (última: `v0.4-demo`) | Desplegada en **https://checklist-choferes.web.app** |
| **Cómo se abre** | Android Studio, carpeta raíz del repo | VS Code, carpeta raíz del repo (así se ve también `CLAUDE.md`/`PROGRESS.md`) |

## Firebase (backend compartido)

Ambas apps hablan directo al mismo proyecto Firebase `checklist-choferes` desde el cliente (sin backend propio, sin Admin SDK):

- **Firestore**: base de datos principal (viajes, catálogos de choferes/camiones/destinos, incidentes).
- **Storage**: fotos del checklist y de incidentes reportados.
- **Hosting**: publica la app Supervisor con un link.
- Plan **Blaze** (pago por uso) activo — necesario porque Storage ya no está en el plan gratuito Spark para proyectos nuevos. El uso real de esta demo cae dentro de la capa gratuita ($0 esperado).
- **Reglas de Firestore en modo prueba** (lectura/escritura abierta) — no hay autenticación todavía. No usar con datos sensibles reales.

## Cómo correr cada app

**Android** (chofer):
1. Abrir la carpeta raíz del repo en Android Studio.
2. Conectar un dispositivo o iniciar un emulador (Android 8.0 / API 26+).
3. `Run` ▶ o `./gradlew installDebug`.
4. Al primer arranque, si Firestore está vacío, la app crea catálogos de prueba automáticamente — no hace falta cargar nada a mano.

**Web** (supervisor):
1. Abrir la carpeta raíz del repo en VS Code.
2. `cd supervisor-web && npm install && npm run dev` → `http://localhost:5173`.
3. Para desplegar cambios: `npm run build && firebase deploy --only hosting` (requiere sesión de `firebase-tools` logueada).

Ninguna de las dos requiere login de usuario dentro de la app — el acceso a Firebase está abierto mientras el proyecto es un prototipo.

## Qué hace cada app

**Android (chofer)**: registro de viaje (chofer, tipo de unidad, placas) → checklist de 18 puntos de inspección + combustible/limpieza + documentación (con fotos por cámara, nunca galería) → itinerario por destino (iniciar/llegar con kilometraje y canastillas) → cargas de combustible → reporte de incidentes. Autoguarda el progreso (borrador local antes de crear el viaje, incremental a Firestore durante el checklist) para no perder nada si se cierra a medio llenar.

**Web (supervisor)**: dashboard con filtros y métricas (combustible cargado, rendimiento, alerta de servicio a 9,000 km) → detalle de cada viaje de solo lectura, con botón para imprimir en el formato Word real que usa la empresa → alerta y resolución de incidentes reportados por choferes → administración (alta/edición/baja) de los catálogos de choferes, camiones y destinos, sin tocar Firestore a mano.

## Más contexto

- [`PROGRESS.md`](./PROGRESS.md): qué está hecho, qué falta, historial de sesiones de desarrollo.
- [`CLAUDE.md`](./CLAUDE.md): el porqué de cada decisión de negocio/UX y el esquema de datos completo — pensado como contexto para asistencia con IA, pero sirve como bitácora técnica del proyecto. Si cambias una decisión de negocio o UX, actualízalo ahí.

## Limitaciones conocidas (por ser prototipo)

- Sin autenticación en ninguna de las dos apps — acceso directo.
- Reglas de Firestore abiertas (modo prueba).
- Sin tests automatizados.
