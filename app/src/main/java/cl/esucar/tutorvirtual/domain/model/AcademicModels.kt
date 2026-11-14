package cl.esucar.tutorvirtual.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DeliverableStatus(val label: String) {
    TODO("Por iniciar"),
    IN_PROGRESS("En desarrollo"),
    SUBMITTED("Enviado"),
    APPROVED("Aprobado")
}

enum class DeliverableUrgency {
    EXPIRED,
    URGENT,
    UPCOMING,
    NORMAL
}

enum class RadioPlaybackState {
    STOPPED,
    BUFFERING,
    PLAYING,
    PAUSED,
    ERROR
}

enum class PomodoroMode(val label: String, val helperText: String, val defaultSeconds: Int) {
    STUDY(
        label = "MÉTODO POMODORO · BLOQUE DE ENFOQUE",
        helperText = "25 min de lectura y redacción sin distracciones",
        defaultSeconds = 25 * 60
    ),
    REST(
        label = "MÉTODO POMODORO · DESCANSO",
        helperText = "5 min para pausar y descansar la vista",
        defaultSeconds = 5 * 60
    )
}

data class Stage(
    val id: String,
    val title: String,
    val description: String,
    val format: String,
    val checklist: List<String>,
    val color: Long
)

data class Deliverable(
    val id: String,
    val title: String,
    val dueDate: String,
    val status: DeliverableStatus,
    val stageId: String
)

data class ProjectTopic(
    val id: String,
    val title: String,
    val createdAt: String
)

data class Faq(
    val question: String,
    val category: String,
    val answer: String
)

data class ApaTemplate(
    val category: String,
    val title: String,
    val example: String,
    val templateFormat: String
)

data class ProgressSummary(
    val fraction: Float = 0f,
    val percentage: Int = 0,
    val approvedDeliverables: Int = 0,
    val completedChecklistItems: Int = 0,
    val totalChecklistItems: Int = 0
)

object InstitutionalLinks {
    const val REVISTA_CARABINEROS = "https://www.revistacarabineros.cl/"
    const val MUSEO_CARABINEROS = "https://www.museocarabineros.cl/web/sitio"
}

val demoApaTemplates = listOf(
    ApaTemplate(
        category = "Leyes y Normativa Chilena",
        title = "Ley Orgánica Constitucional de Carabineros (Ley 18.961)",
        example = "Ministerio de Defensa Nacional. (1990, 7 de marzo). Ley 18961: Ley Orgánica Constitucional de Carabineros de Chile. Biblioteca del Congreso Nacional de Chile.",
        templateFormat = "Entidad emisora. (Año, Día de Mes). Ley N°: Título oficial de la ley. Sitio/Repositorio."
    ),
    ApaTemplate(
        category = "Reglamentos Institucionales",
        title = "Reglamento de Disciplina de Carabineros (N° 11)",
        example = "Carabineros de Chile. (2018). Reglamento de Disciplina de Carabineros, N° 11. Dirección General de Carabineros.",
        templateFormat = "Carabineros de Chile. (Año). Nombre del Reglamento, N° XX. Unidad/Dirección Emisora."
    ),
    ApaTemplate(
        category = "Libros y Manuales Académicos",
        title = "Metodología de la Investigación Policial",
        example = "Hernández Sampieri, R., & Mendoza, C. P. (2018). Metodología de la investigación: las rutas cuantitativa, cualitativa y mixta. McGraw-Hill.",
        templateFormat = "Apellido, N. (Año). Título del libro en cursiva (Edición). Editorial."
    ),
    ApaTemplate(
        category = "Artículos Científicos / Revistas Policiales",
        title = "Artículo en Revista de Seguridad Pública",
        example = "Pérez, J., & Soto, M. (2022). Innovación en la gestión cuadrante de seguridad preventiva. Revista de Criminología y Ciencias Policiales, 15(2), 45-62.",
        templateFormat = "Apellido, N. (Año). Título del artículo. Nombre de la Revista, Volumen(Número), páginas."
    )
)

val demoStages = listOf(
    Stage(
        "planteamiento",
        "Planteamiento",
        "Delimita el problema, la pregunta de investigación y los objetivos del trabajo.",
        "Avance escrito, 2 a 4 páginas, interlineado 1,5.",
        listOf("Contexto y problema descritos", "Pregunta de investigación definida", "Objetivo general y específicos revisados"),
        0xFF218265
    ),
    Stage(
        "marco",
        "Marco Teórico",
        "Organiza los conceptos, antecedentes y fuentes que sustentan tu propuesta.",
        "Documento con citas y referencias en norma APA 7.",
        listOf("Fuentes académicas seleccionadas", "Conceptos clave definidos", "Citas y referencias verificadas"),
        0xFF17654F
    ),
    Stage(
        "metodologia",
        "Metodología",
        "Explica cómo responderás la pregunta y cómo analizarás la información.",
        "Capítulo metodológico con enfoque, alcance e instrumentos.",
        listOf("Enfoque y diseño definidos", "Población y muestra descritas", "Instrumento de recolección preparado"),
        0xFF8BAF3A
    ),
    Stage(
        "recogida",
        "Recogida de información",
        "Aplica los instrumentos respetando criterios éticos y registra evidencias.",
        "Base de datos o matriz de evidencias, con respaldo local.",
        listOf("Consentimientos registrados", "Instrumentos aplicados", "Matriz de datos ordenada"),
        0xFF1F4768
    ),
    Stage(
        "analisis",
        "Análisis de resultados",
        "Interpreta los hallazgos y relaciónalos con tus objetivos y marco teórico.",
        "Informe con tablas, gráficos y discusión.",
        listOf("Datos depurados", "Resultados visualizados", "Conclusiones conectadas a objetivos"),
        0xFF3A6B5A
    )
)

val demoDeliverables = listOf(
    Deliverable("d1", "Avance de planteamiento", "15 nov 2026", DeliverableStatus.IN_PROGRESS, "planteamiento"),
    Deliverable("d2", "Marco teórico preliminar", "29 nov 2026", DeliverableStatus.TODO, "marco"),
    Deliverable("d3", "Propuesta metodológica", "10 dic 2026", DeliverableStatus.SUBMITTED, "metodologia"),
    Deliverable("d4", "Informe final", "20 dic 2026", DeliverableStatus.TODO, "analisis")
)

val demoFaqs = listOf(
    Faq("¿Cómo formulo una pregunta de investigación?", "Metodología", "Debe ser clara, delimitada y responder a un problema observable en el ámbito policial o de seguridad. Evita preguntas que solo se respondan con sí o no."),
    Faq("¿Qué diferencia hay entre objetivo general y específicos?", "Metodología", "El objetivo general expresa el propósito central del trabajo. Los específicos lo descomponen en acciones observables y ordenadas."),
    Faq("¿Cómo cito una ley chilena o reglamento policial en APA 7?", "Citación APA", "Para leyes: Ministerio de Defensa Nacional. (Año). Ley N° X. Sitio BCN. Para reglamentos: Carabineros de Chile. (Año). Reglamento N° X."),
    Faq("¿Cómo cito una página web en APA 7?", "Citación APA", "Autor. (Año). Título de la página. Sitio web. URL. Si no hay fecha, usa (s. f.)."),
    Faq("¿Cuándo uso una cita textual en mi Trabajo de Título?", "Citación APA", "Úsala cuando las palabras exactas sean indispensables (artículos de leyes, normas o definiciones oficiales). Agrega autor, año y página/párrafo."),
    Faq("¿Cuál es el formato recomendado para el avance?", "Formato", "Usa letra legible, títulos jerárquicos, interlineado 1,5, márgenes regulares y referencias consistentes."),
    Faq("¿Cómo aplico el Método Pomodoro a mi estudio?", "Estudio", "Trabaja 25 minutos sin distracciones en tu avance académico y descansa 5 minutos. Después de 4 bloques, toma un descanso más largo de 15-30 minutos."),
    Faq("¿Qué es la Matriz de Consistencia?", "Metodología", "Es un cuadro resumen que conecta: Problema → Pregunta → Objetivo General → Objetivos Específicos → Metodología. Ayuda a mantener coherencia en tu proyecto.")
)

val faqCategories = listOf("Todas", "Metodología", "Citación APA", "Formato", "Estudio")

fun calculateUrgency(dueDateStr: String): DeliverableUrgency {
    val formats = listOf(
        SimpleDateFormat("d MMM yyyy", Locale("es", "ES")),
        SimpleDateFormat("d MMM yyyy", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT),
        SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)
    )

    var parsedDate: Date? = null
    for (fmt in formats) {
        val parsed = runCatching { fmt.parse(dueDateStr) }.getOrNull()
        if (parsed != null) {
            parsedDate = parsed
            break
        }
    }

    if (parsedDate == null) return DeliverableUrgency.NORMAL

    val now = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    val diffDays = (parsedDate.time - now.time) / (1000 * 60 * 60 * 24)
    return when {
        diffDays < 0 -> DeliverableUrgency.EXPIRED
        diffDays <= 3 -> DeliverableUrgency.URGENT
        diffDays <= 7 -> DeliverableUrgency.UPCOMING
        else -> DeliverableUrgency.NORMAL
    }
}

fun generateApaCitation(
    type: String,
    author: String,
    year: String,
    title: String,
    source: String
): String {
    val cleanAuthor = author.ifBlank { "Carabineros de Chile" }
    val cleanYear = year.ifBlank { "s. f." }
    val cleanTitle = title.ifBlank { "Trabajo de Investigación" }
    val cleanSource = source.ifBlank { "Repositorio Institucional ESUCAR" }

    return when (type) {
        "Ley" -> "$cleanAuthor. ($cleanYear). Ley $cleanTitle. $cleanSource."
        "Reglamento" -> "Carabineros de Chile. ($cleanYear). Reglamento de $cleanTitle, N° $cleanSource. Dirección General de Carabineros."
        "Libro" -> "$cleanAuthor. ($cleanYear). $cleanTitle. $cleanSource."
        else -> "$cleanAuthor. ($cleanYear). $cleanTitle. $cleanSource."
    }
}
