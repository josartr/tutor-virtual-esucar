package cl.esucar.tutorvirtual.domain.model

import java.text.Normalizer
import java.util.Locale

enum class InstitutionType(val label: String) {
    ESUCAR("ESUCAR"),
    CARABINEROS("Carabineros de Chile")
}

enum class UserRole(val label: String) {
    ESTUDIANTE("Estudiante"),
    FUNCIONARIO("Funcionario")
}

data class UserProfile(
    val rank: String = "Alumno ESUCAR",
    val firstName: String = "",
    val firstLastName: String = "",
    val secondLastName: String = "",
    val fullName: String = buildFullName(firstName, firstLastName, secondLastName),
    val email: String = "",
    val researchTopic: String = "",
    val tutorName: String = "",
    val ssoProvider: String = "Correo Institucional",
    val institutionType: InstitutionType = InstitutionType.ESUCAR,
    val userRole: UserRole = UserRole.ESTUDIANTE,
    val isLoggedIn: Boolean = false
)

sealed interface AuthResult {
    data class Success(val profile: UserProfile) : AuthResult
    data object EmailAlreadyRegistered : AuthResult
    data object InvalidCredentials : AuthResult
    data object InvalidRegistration : AuthResult
    data object PersistenceError : AuthResult
}

val defaultRanks = listOf(
    "Alumno ESUCAR",
    "Cabo 2°",
    "Cabo 1°",
    "Sargento 2°",
    "Sargento 1°",
    "Suboficial",
    "Oficial Subalterno / Asesor"
)

val defaultEsucarTutors = listOf(
    "Prof. Dr. Nelson Soto V. (Metodología ESUCAR)",
    "Dra. Patricia Morales G. (Seguridad Pública)",
    "Coronel (R) Carlos Ibáñez M. (Gestión Policial)",
    "Mayor Jaime Fuentes R. (Prevención e Investigaciones)",
    "Prof. Francisco Araya T. (Derecho y Normativa Policial)",
    "Capitán Andrea Silva L. (Criminología Operacional)",
    "Prof. Marcela Reyes B. (Redacción Académica y APA)"
)

fun buildFullName(firstName: String, firstLastName: String, secondLastName: String): String =
    listOf(firstName.trim(), firstLastName.trim(), secondLastName.trim())
        .filter { it.isNotBlank() }
        .joinToString(" ")

fun institutionForEmail(email: String): InstitutionType? {
    val clean = email.trim().lowercase(Locale.ROOT)
    val parts = clean.split("@")
    if (parts.size != 2 || parts[0].isBlank()) return null
    return when (parts[1]) {
        "esucar.cl" -> InstitutionType.ESUCAR
        "carabineros.cl" -> InstitutionType.CARABINEROS
        else -> null
    }
}

fun isValidInstitutionalEmail(email: String): Boolean = institutionForEmail(email) != null

fun isEmailForInstitution(email: String, institutionType: InstitutionType): Boolean =
    institutionForEmail(email) == institutionType

fun resolveProfileRank(
    institutionType: InstitutionType,
    userRole: UserRole,
    selectedRank: String
): String = when (institutionType) {
    InstitutionType.CARABINEROS -> {
        val clean = selectedRank.trim()
        if (clean.isBlank() || clean == "Alumno ESUCAR" || clean == "Funcionario ESUCAR") {
            defaultRanks[1]
        } else {
            clean
        }
    }
    InstitutionType.ESUCAR -> {
        if (userRole == UserRole.FUNCIONARIO) "Funcionario ESUCAR" else "Alumno ESUCAR"
    }
}

fun sanitizeEmailToken(input: String): String {
    val normalized = Normalizer.normalize(input.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
    return normalized
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        .replace(Regex("[^a-z0-9]"), "")
}

fun generateInstitutionalEmail(
    firstName: String,
    firstLastName: String,
    secondLastName: String,
    userRole: UserRole
): String {
    val firstToken = firstName.trim()
        .split(Regex("\\s+"))
        .firstOrNull()
        ?.let(::sanitizeEmailToken)
        .orEmpty()
    val last1Token = firstLastName.trim()
        .split(Regex("\\s+"))
        .joinToString("", transform = ::sanitizeEmailToken)
    val last2Initial = secondLastName.trim()
        .split(Regex("\\s+"))
        .firstOrNull()
        ?.let(::sanitizeEmailToken)
        ?.firstOrNull()
        ?.toString()
        .orEmpty()

    val localParts = buildList {
        if (firstToken.isNotBlank()) add(firstToken)
        if (last1Token.isNotBlank()) add(last1Token)
        if (firstToken.isNotBlank() && last1Token.isNotBlank() && last2Initial.isNotBlank()) {
            add(last2Initial)
        }
    }
    if (localParts.isEmpty()) return ""
    val domain = when (userRole) {
        UserRole.ESTUDIANTE -> "esucar.cl"
        UserRole.FUNCIONARIO -> "carabineros.cl"
    }
    return "${localParts.joinToString(".")}@$domain"
}
