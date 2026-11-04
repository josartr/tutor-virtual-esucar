<div align="center">

<img src="app/src/main/res/drawable-nodpi/institutional_header_logo.png" alt="Carabineros de Chile - ESUCAR" width="120" />

# 🛡️ Tutor Virtual ESUCAR 🇨🇱
### *Escuela de Suboficiales de Carabineros de Chile "SOM Fabriciano González Urzúa"*
**Plataforma Móvil Educativa de Acompañamiento Metodológico para Trabajos de Título**

![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-007328?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-007328?style=for-the-badge&logo=kotlin&logoColor=white)
![Material 3](https://img.shields.io/badge/Material--3-Design-007328?style=for-the-badge&logo=google&logoColor=white)
![Offline First](https://img.shields.io/badge/Offline--First-100%25-007328?style=for-the-badge&logo=sqlite&logoColor=white)

---

### *"ORDEN Y PATRIA"*
</div>

---

## 👥 Integrantes del Equipo

- 👮 **José Arturo T.**
- 👮 **Martin Hernandez**

---

## 📌 Descripción General

**Tutor Virtual ESUCAR** es una solución tecnológica móvil creada para orientar y acompañar a los funcionarios y alumnos de la **Escuela de Suboficiales de Carabineros de Chile (ESUCAR)** durante la investigación y redacción de su **Trabajo de Título**. 

La aplicación permite conciliar la alta exigencia operativa de la función policial con el rigor académico, proporcionando herramientas metodológicas autónomas, gestión de hitos, temporizador de estudio y soporte de citación normativa **100% accesible sin conexión a internet**.

---

## ✨ Características Principales

### 🔐 1. Autenticación SSO e Inicio de Sesión Institucional
- **Acceso Rápido SSO (Single Sign-On):** Autenticación en un toque mediante la Identidad Digital de Carabineros de Chile (`@carabineros.cl` o `@esucar.cl`) o ClaveÚnica Institucional.
- **Validación de Correo en Tiempo Real:** Detección de dominios oficiales con distintivo visual de *Dominio Verificado*.
- **Perfil Personalizado:** Registro de Grado Policial (*Cabo 1°, Sargento 2°, Suboficial, Alumno*), Nombre Completo, Tema de Investigación y Profesor Tutor asignado.

### 🎓 2. Listado Oficial de 7 Profesores Tutores ESUCAR
Selección directa de los docentes especialistas oficializados por ESUCAR:
1. **Prof. Dr. Nelson Soto V.** *(Metodología ESUCAR)*
2. **Dra. Patricia Morales G.** *(Seguridad Pública)*
3. **Coronel (R) Carlos Ibáñez M.** *(Gestión Policial)*
4. **Mayor Jaime Fuentes R.** *(Prevención e Investigaciones)*
5. **Prof. Francisco Araya T.** *(Derecho y Normativa Policial)*
6. **Capitán Andrea Silva L.** *(Criminología Operacional)*
7. **Prof. Marcela Reyes B.** *(Redacción Académica y APA 7)*

### ⏱️ 3. Módulo de Herramientas de Estudio (Pestaña "Estudio")
- **Método Pomodoro de Enfoque:** Bloques de 25 minutos de lectura/redacción intensa con 5 minutos de descanso, diseñados para optimizar breves ventanas de tiempo entre turnos operativos.
- **Generador de Citas APA 7 Institucional:** Formateador automático con plantillas para **Leyes Chilenas** *(Ley Orgánica 18.961)*, **Reglamentos de Carabineros de Chile** *(Reglamento de Disciplina N° 11)* y Libros académicos, con botón de copiado al portapapeles.
- **Matriz de Consistencia Metodológica:** Guía interactiva de alineación entre Problema, Pregunta, Objetivo General, Objetivos Específicos y Metodología.

### 📋 4. Guía Metodológica y Bitácora Personal por Etapa
- **5 Etapas Estructuradas:** *Planteamiento, Marco Teórico, Metodología, Recogida de Información y Análisis de Resultados*.
- **Avance Visible:** Indicador de tareas completadas por etapa (ej. `2 de 3 tareas completadas`) con mini barra de progreso.
- **Bitácora del Estudiante:** Espacio de notas personales con autoguardado permanente para registrar consultas hacia el profesor tutor.
- **Evidencias Nativas:** Captura directa con cámara fotográfica o archivo adjunto de avance.

### 📅 5. Gestión Completa de Hitos Académicos (Deliverables)
- Operaciones complejas de **Crear, Editar, Modificar Estado y Eliminar Hitos** con confirmación.
- **Etiquetas de Urgencia Automáticas:** Identificación visual de entregas *(Vence pronto <= 3 días, Próximo <= 7 días, Vencido, A tiempo)*.
- **Notificaciones Locales:** Recordatorios del sistema con `WorkManager` para evitar retrasos académicos.

### 🎨 6. Identidad Visual Institucional `#007328`
- **Color Oficial Carabineros (`#007328`):** Paleta corporativa con acento Verde Lima (`#8BAF3A`), Azul (`#1F4768`) y Blanco.
- **Footer Compacto Optimizado:** Navegación inferior basada en `Surface` + `Row` (`56.dp`) que maximiza el área útil de lectura y elimina recortes de íconos.

---

## 🛠️ Arquitectura y Tecnologías

```
cl.esucar.tutorvirtual
│
├── data/                       # Capa de Datos y Modelos
│   ├── Models.kt               # UserProfile, Stage, Deliverable, ApaTemplate, Urgency logic
│   ├── LocalRepository.kt      # Persistencia offline en SharedPreferences
│   └── ProgressCalculator.kt   # Algoritmo de cálculo de avance ponderado
│
├── ui/                         # Capa de Interfaz y Estado
│   ├── InstitutionalTheme.kt   # Paleta corporativa (#007328, M3 LightColors)
│   └── TutorViewModel.kt       # Gestión de estado (StateFlow) y temporizador Pomodoro
│
└── MainActivity.kt             # UI Jetpack Compose, Login, SSO Modal, Screens y Footer
```

- **UI:** Jetpack Compose + Material 3 Components.
- **Arquitectura:** MVVM (Model-View-ViewModel) con Kotlin `StateFlow`.
- **Persistencia:** `LocalRepository` (`SharedPreferences` + JSON).
- **Procesos en Segundo Plano:** `WorkManager` + `NotificationCompat`.
- **Integraciones Nativas:** Cámara Android, Storage Access Framework y Clipboard.

---

## 🚀 Ejecutar el Proyecto en Android Studio

1. Clonar o abrir la carpeta raíz del proyecto en **Android Studio Jellyfish / Iguana / Hedgehog**.
2. Sincronizar las dependencias de Gradle con **JDK 17**.
3. Seleccionar o crear un emulador **Android API 26+** (o conectar un dispositivo físico con Depuración USB).
4. Ejecutar la configuración de inicio **`app`**.
5. *Opcional:* Aceptar el permiso de Notificaciones y Cámara cuando la app lo solicite.

---

## 🧪 Pruebas Unitarias Automatizadas

El proyecto cuenta con cobertura de pruebas unitarias automatizadas para validar la lógica del perfil SSO, generador APA 7, cálculo de urgencias y avance metodológico.

Para ejecutar la suite de pruebas desde la terminal:
```powershell
./gradlew test
```

Para generar la compilación del APK Debug:
```powershell
./gradlew :app:assembleDebug
```

---

<div align="center">
<b>Escuela de Suboficiales de Carabineros de Chile (ESUCAR)</b><br/>
<i>"Formando a los Líderes Operacionales de la Función Policial"</i>
</div>
