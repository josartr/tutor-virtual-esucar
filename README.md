# Tutor Virtual ESUCAR

MVP móvil para estudiantes de ESUCAR que desarrollan su Trabajo de Título. La aplicación centraliza la guía metodológica, preguntas frecuentes, fechas de entrega y avance de hitos para apoyar el trabajo autónomo fuera del aula.

## Estudiantes

- Estudiante 1: **[Nombre y apellido]**
- Estudiante 2: **[Nombre y apellido]**
- Equipo: **[Número de equipo]**

## Funcionalidades

- Dashboard con progreso animado y próximo hito.
- Guía de cinco etapas: Planteamiento, Marco Teórico, Metodología, Recogida de información y Análisis de resultados.
- Detalle de cada etapa con descripción, formato requerido y checklist.
- FAQ con búsqueda y categorías: Metodología, Citación APA y Formato.
- Gestión de hitos con estados Por iniciar, En desarrollo, Enviado y Aprobado.
- Formulario de nuevo hito con validación por campo, mensajes e íconos.
- Persistencia local de hitos para uso offline y datos sobrevivientes a reinicios.
- Recordatorio local de fechas límite.
- Recursos nativos: cámara para evidencia y selector seguro de documentos.
- Animaciones de entrada, expansión de respuestas y progreso.

## Ejecutar en Android Studio

1. Abrir la carpeta raíz en Android Studio Hedgehog (2023.1.1) o superior.
2. Esperar la sincronización de Gradle con JDK 17 o superior.
3. Crear o seleccionar un emulador Android API 26+ (o conectar un dispositivo con depuración USB).
4. Ejecutar la configuración `app`.
5. Para probar cámara y notificaciones, aceptar los permisos solicitados por el sistema.

El proyecto no requiere servidor: el contenido de demostración se carga localmente y los cambios de hitos se guardan en `SharedPreferences`.

## Arquitectura

La app usa Jetpack Compose, Navigation Compose y un `TutorViewModel` que concentra estado y validaciones de flujo. `LocalRepository` abstrae la persistencia local y los modelos de `data` mantienen separadas las responsabilidades de dominio de la UI.
