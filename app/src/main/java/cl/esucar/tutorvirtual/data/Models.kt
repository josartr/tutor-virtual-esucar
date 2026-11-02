package cl.esucar.tutorvirtual.data

enum class DeliverableStatus(val label: String) {
    TODO("Por iniciar"), IN_PROGRESS("En desarrollo"), SUBMITTED("Enviado"), APPROVED("Aprobado")
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

data class Faq(val question: String, val category: String, val answer: String)

val demoStages = listOf(
    Stage("planteamiento", "Planteamiento", "Delimita el problema, la pregunta de investigación y los objetivos del trabajo.", "Avance escrito, 2 a 4 páginas, interlineado 1,5.", listOf("Contexto y problema descritos", "Pregunta de investigación definida", "Objetivo general y específicos revisados"), 0xFF218265),
    Stage("marco", "Marco Teórico", "Organiza los conceptos, antecedentes y fuentes que sustentan tu propuesta.", "Documento con citas y referencias en norma APA 7.", listOf("Fuentes académicas seleccionadas", "Conceptos clave definidos", "Citas y referencias verificadas"), 0xFF17654F),
    Stage("metodologia", "Metodología", "Explica cómo responderás la pregunta y cómo analizarás la información.", "Capítulo metodológico con enfoque, alcance e instrumentos.", listOf("Enfoque y diseño definidos", "Población y muestra descritas", "Instrumento de recolección preparado"), 0xFF8BAF3A),
    Stage("recogida", "Recogida de información", "Aplica los instrumentos respetando criterios éticos y registra evidencias.", "Base de datos o matriz de evidencias, con respaldo local.", listOf("Consentimientos registrados", "Instrumentos aplicados", "Matriz de datos ordenada"), 0xFF1F4768),
    Stage("analisis", "Análisis de resultados", "Interpreta los hallazgos y relaciónalos con tus objetivos y marco teórico.", "Informe con tablas, gráficos y discusión.", listOf("Datos depurados", "Resultados visualizados", "Conclusiones conectadas a objetivos"), 0xFF3A6B5A)
)

val demoDeliverables = listOf(
    Deliverable("d1", "Avance de planteamiento", "15 nov 2026", DeliverableStatus.IN_PROGRESS, "planteamiento"),
    Deliverable("d2", "Marco teórico preliminar", "29 nov 2026", DeliverableStatus.TODO, "marco"),
    Deliverable("d3", "Propuesta metodológica", "10 dic 2026", DeliverableStatus.SUBMITTED, "metodologia"),
    Deliverable("d4", "Informe final", "20 dic 2026", DeliverableStatus.TODO, "analisis")
)

val demoFaqs = listOf(
    Faq("¿Cómo formulo una pregunta de investigación?", "Metodología", "Debe ser clara, delimitada y responder a un problema observable. Evita preguntas que solo se respondan con sí o no."),
    Faq("¿Qué diferencia hay entre objetivo general y específicos?", "Metodología", "El objetivo general expresa el propósito central. Los específicos lo descomponen en acciones observables y ordenadas."),
    Faq("¿Cómo cito una página web en APA 7?", "Citación APA", "Autor. (Año). Título de la página. Sitio web. URL. Si no hay fecha, usa (s. f.)."),
    Faq("¿Cuándo uso una cita textual?", "Citación APA", "Úsala cuando las palabras exactas sean relevantes. Agrega autor, año y página o párrafo."),
    Faq("¿Cuál es el formato recomendado para el avance?", "Formato", "Usa letra legible, títulos jerárquicos, interlineado 1,5, márgenes regulares y referencias consistentes.")
)
