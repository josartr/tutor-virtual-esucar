<div align="center">

<img src="app/src/main/res/drawable-nodpi/institutional_header_logo.png" alt="Carabineros de Chile - ESUCAR" width="120" />

# 🛡️ Tutor Virtual ESUCAR 🇨🇱
### *Escuela de Suboficiales de Carabineros de Chile "SOM Fabriciano González Urzúa"*
**Plataforma Móvil Nativa de Acompañamiento Metodológico para Trabajos de Título**

![Android](https://img.shields.io/badge/Android-API%2026--35-1B5E3F?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-1B5E3F?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-1B5E3F?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Media3](https://img.shields.io/badge/Media3-ExoPlayer%20%26%20Session-1B5E3F?style=for-the-badge)
![Offline First](https://img.shields.io/badge/SQLite-PBKDF2--SHA256-1B5E3F?style=for-the-badge&logo=sqlite&logoColor=white)

---

### *"ORDEN Y PATRIA"*
</div>

---

## 👥 Integrantes del Equipo

- 👮 **José Arturo T.**
- 👮 **Martín Hernández**

---

## 📌 Descripción General

**Tutor Virtual ESUCAR** es una aplicación móvil nativa en **Kotlin y Jetpack Compose** creada para orientar y acompañar a los estudiantes y funcionarios de la **Escuela de Suboficiales de Carabineros de Chile (ESUCAR)** durante la investigación y redacción de su **Trabajo de Título**.

La plataforma combina un diseño institucional suave anti-fatiga visual con persistencia **100% offline-first en SQLite** protegida criptográficamente, herramientas académicas (APA 7, Pomodoro, bitácora y gestión de hitos) e integración multimedia en vivo con **Radio Carabineros de Chile** en segundo plano mediante **AndroidX Media3**.

---

## ✨ Características Principales

### 🔐 1. Login Unificado, Roles y Generación Automática de Correo
- **Acceso unificado (`@esucar.cl` / `@carabineros.cl`):** Detección automática del dominio institucional sin tarjetas redundantes previas.
- **Selector de Rol (`Estudiante` / `Funcionario`):**
  - **Estudiante:** asigna automáticamente dominio `@esucar.cl` y rango `Alumno ESUCAR`.
  - **Funcionario:** asigna automáticamente dominio `@carabineros.cl` y habilita el selector obligatorio de **Grado policial** (*Cabo 2°, Cabo 1°, Sargento 2°, Sargento 1°, Suboficial, Oficial Subalterno / Asesor*).
- **Generación RFC/ASCII en tiempo real:** Al completar **Nombres**, **1er Apellido** y **2do Apellido** separados, normaliza tildes y caracteres especiales mediante Unicode `NFD` para proponer automáticamente el correo institucional (p. ej., `maria.oconnor.n@esucar.cl` o `marcos.soto.r@carabineros.cl`), manteniendo edición manual opcional.
- **Seguridad criptográfica local:** Base de datos **SQLite (v6)** con derivación `PBKDF2WithHmacSHA256` (120.000 iteraciones, `SecureRandom` salt de 16 bytes, comparación constante `MessageDigest.isEqual` y limpieza de memoria `clearPassword()`).
- **Superposición de carga global (`GlobalLoadingOverlay`):** Bloquea interacciones accidentales durante autenticación, guardado de perfil y conexión multimedia.

### 📚 2. Registro Inmutable de Temas de Proyecto
- Los temas de proyecto registrados en SQLite (`project_topics`) **no son modificables** por diseño (solo permiten **añadir** nuevos temas o **eliminar** con confirmación), resguardando la trazabilidad académica tanto en Inicio como en el Perfil de usuario.

### 📻 3. Radio Carabineros en Vivo y Accesos Rápidos Institucionales
- **Reproducción nativa en segundo plano (Media3):** Implementada con `RadioPlaybackService` (`MediaSessionService` + `ExoPlayer` + `WAKE_MODE_NETWORK`), manejando foco de audio y audio en vivo (`FM 98.1 / AM 820`).
- **Notificación persistente Play/Pause:** Generada por `DefaultMediaNotificationProvider` con ícono institucional, controles en barra de notificaciones/pantalla de bloqueo y sincronización bidireccional mediante `MediaController`.
- **Barra multimedia personalizada (`MiniRadioBar`):** Permanece visible sobre la barra inferior para reproducir, pausar, reintentar, silenciar y ajustar el volumen con precisión en tiempo real.
- **Accesos rápidos dedicados:** **Radio Carabineros**, **Revista Carabineros** y **Museo de Carabineros** (con declaración `<queries>` para `ACTION_VIEW` en Android 11–15).

### 🎓 4. Listado Oficial de 7 Profesores Tutores ESUCAR
1. **Prof. Dr. Nelson Soto V.** *(Metodología ESUCAR)*
2. **Dra. Patricia Morales G.** *(Seguridad Pública)*
3. **Coronel (R) Carlos Ibáñez M.** *(Gestión Policial)*
4. **Mayor Jaime Fuentes R.** *(Prevención e Investigaciones)*
5. **Prof. Francisco Araya T.** *(Derecho y Normativa Policial)*
6. **Capitán Andrea Silva L.** *(Criminología Operacional)*
7. **Prof. Marcela Reyes B.** *(Redacción Académica y APA 7)*

### ⏱️ 5. Herramientas Académicas, Etapas y Hitos
- **5 Etapas con checklist, notas y evidencias:** *Planteamiento, Marco Teórico, Metodología, Recogida de información y Análisis de resultados*, con cálculo ponderado en tiempo real (`ProgressCalculator`).
- **Módulo de Estudio:** Temporizador **Pomodoro (25/5 min)**, **Generador de Citas APA 7** para leyes chilenas y reglamentos policiales, y **Matriz de Consistencia**.
- **Gestión de Hitos y Recordatorios:** CRUD de entregables con etiquetas de urgencia y notificaciones diarias mediante `WorkManager`.

### 🎨 6. Identidad Visual Anti-Fatiga e Ícono Adaptativo del APK
- **Paleta institucional suave (`#1B5E3F`, `#F6F8F6`, `#EAF2ED`):** Reduce la fatiga visual; banner de login invertido mediante `ColorFilter.colorMatrix` sobre tarjeta blanca y footer Edge-to-Edge con `navigationBarsPadding()`.
- **Ícono adaptativo propio del APK:** Escudo institucional ESUCAR en esmeralda, marfil y dorado centrado en la zona segura adaptativa (`mipmap-anydpi-v26`).

---

## 🛠️ Arquitectura y Estructura del Proyecto

```
├── app/src/main/java/cl/esucar/tutorvirtual/
│   ├── data/
│   │   ├── Models.kt                # UserProfile, UserRole, generador RFC de correos, APA 7, FAQs
│   │   ├── LocalRepository.kt       # SQLite v6, PBKDF2-SHA256, project_topics inmutables, hitos
│   │   └── ProgressCalculator.kt    # Algoritmo de cálculo ponderado de avance
│   ├── radio/
│   │   └── RadioPlaybackService.kt  # Media3 MediaSessionService + notificación persistente Play/Pause
│   ├── ui/
│   │   ├── InstitutionalTheme.kt    # Paleta institucional anti-fatiga visual (Material 3)
│   │   └── TutorViewModel.kt        # StateFlow, MediaController Media3, Pomodoro y auth async
│   ├── DeadlineReminderWorker.kt    # Recordatorios diarios con WorkManager
│   └── MainActivity.kt              # Jetpack Compose UI, Login unificado, MiniRadioBar y Edge-to-Edge
├── app/src/test/java/cl/esucar/tutorvirtual/data/
│   ├── ProgressCalculatorTest.kt    # Pruebas unitarias de cálculo ponderado
│   └── StudyToolsTest.kt            # Pruebas unitarias de dominios, roles, correos, APA 7 y urgencias
└── docs/
    ├── Presentacion_Tutor_Virtual_ESUCAR.pptx  # Presentación PowerPoint (8 diapositivas + guion)
    └── presentacion.html                       # Presentación web interactiva + código crítico + guion
```

- **Lenguaje y UI:** Kotlin `2.0.21` + Jetpack Compose BOM `2024.12.01` + Material 3.
- **Multimedia:** `androidx.media3:media3-exoplayer:1.5.1` y `androidx.media3:media3-session:1.5.1`.
- **Persistencia y Concurrencia:** SQLite nativo + Coroutines (`StateFlow`) + `WorkManager`.
- **SDK:** `minSdk = 26`, `compileSdk = 35`, `targetSdk = 35`.

---

## 📊 Material de Presentación (José y Martín)

- **PowerPoint descargable:** [`docs/Presentacion_Tutor_Virtual_ESUCAR.pptx`](docs/Presentacion_Tutor_Virtual_ESUCAR.pptx) *(8 diapositivas con extractos de código críticos, guion en pantalla y notas del orador).*
- **Presentación Web interactiva:** [`docs/presentacion.html`](docs/presentacion.html) *(navegación por teclado `←`/`→` y modo de impresión PDF).*

---

## 🚀 Compilación y Pruebas Unitarias

1. Abrir el proyecto en **Android Studio** con **JDK 21**.
2. Ejecutar las pruebas unitarias automatizadas:
```bash
./gradlew testDebugUnitTest
```
3. Verificar análisis estático Lint:
```bash
./gradlew lintDebug
```
4. Generar el APK Debug firmado:
```bash
./gradlew :app:assembleDebug
```

---

<div align="center">
<b>Escuela de Suboficiales de Carabineros de Chile (ESUCAR)</b><br/>
<i>"Formando a los Líderes Operacionales de la Función Policial"</i>
</div>
