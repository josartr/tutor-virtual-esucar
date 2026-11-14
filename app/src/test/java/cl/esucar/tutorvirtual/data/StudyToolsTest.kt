package cl.esucar.tutorvirtual.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyToolsTest {

    @Test
    fun `user profile defaults to not logged in`() {
        val defaultProfile = UserProfile()
        assertEquals("Alumno ESUCAR", defaultProfile.rank)
        assertEquals("", defaultProfile.fullName)
        assertEquals(false, defaultProfile.isLoggedIn)
    }

    @Test
    fun `default esucar tutors list has exactly 7 official tutors`() {
        assertEquals(7, defaultEsucarTutors.size)
        assertTrue(defaultEsucarTutors.any { it.contains("Nelson Soto") })
    }

    @Test
    fun `isValidInstitutionalEmail accepts institutional domains`() {
        assertTrue(isValidInstitutionalEmail("suboficial.soto@carabineros.cl"))
        assertTrue(isValidInstitutionalEmail("alumno.perez@esucar.cl"))
        assertTrue(isValidInstitutionalEmail("contacto@carabineros.cl"))
        assertFalse(isValidInstitutionalEmail("sin_arroba_ni_punto"))
        assertFalse(isValidInstitutionalEmail("usuario@otra-institucion.cl"))
        assertFalse(isValidInstitutionalEmail("usuario@esucar.cl.ejemplo.com"))
        assertEquals(InstitutionType.ESUCAR, institutionForEmail("alumno@esucar.cl"))
        assertEquals(InstitutionType.CARABINEROS, institutionForEmail("FUNCIONARIO@CARABINEROS.CL"))
        assertEquals(null, institutionForEmail("usuario@gmail.com"))
        assertTrue(isEmailForInstitution("alumno@esucar.cl", InstitutionType.ESUCAR))
        assertTrue(isEmailForInstitution("funcionario@carabineros.cl", InstitutionType.CARABINEROS))
        assertFalse(isEmailForInstitution("alumno@esucar.cl", InstitutionType.CARABINEROS))
    }

    @Test
    fun `resolveProfileRank respects domain and user role`() {
        assertEquals(
            "Alumno ESUCAR",
            resolveProfileRank(InstitutionType.ESUCAR, UserRole.ESTUDIANTE, "Sargento 1°")
        )
        assertEquals(
            "Funcionario ESUCAR",
            resolveProfileRank(InstitutionType.ESUCAR, UserRole.FUNCIONARIO, "")
        )
        assertEquals(
            "Sargento 2°",
            resolveProfileRank(InstitutionType.CARABINEROS, UserRole.FUNCIONARIO, "Sargento 2°")
        )
        assertEquals(
            defaultRanks[1],
            resolveProfileRank(InstitutionType.CARABINEROS, UserRole.ESTUDIANTE, "Alumno ESUCAR")
        )
    }

    @Test
    fun `generateInstitutionalEmail normalizes names and assigns domain by role`() {
        assertEquals(
            "maria.oconnor.n@esucar.cl",
            generateInstitutionalEmail("María José", "O'Connor", "Núñez", UserRole.ESTUDIANTE)
        )
        assertEquals(
            "marcos.soto.r@carabineros.cl",
            generateInstitutionalEmail("Marcos", "Soto", "Rojas", UserRole.FUNCIONARIO)
        )
        assertEquals(
            "juan.delacruz@esucar.cl",
            generateInstitutionalEmail("Juan", "De La Cruz", "", UserRole.ESTUDIANTE)
        )
        assertEquals(
            "",
            generateInstitutionalEmail("   ", "", "", UserRole.ESTUDIANTE)
        )
    }

    @Test
    fun `apa citation generator formats ley correctly`() {
        val citation = generateApaCitation(
            type = "Ley",
            author = "Ministerio de Defensa Nacional",
            year = "1990",
            title = "18961 Orgánica Constitucional de Carabineros",
            source = "Biblioteca del Congreso Nacional"
        )

        assertTrue(citation.contains("Ministerio de Defensa Nacional"))
        assertTrue(citation.contains("(1990)"))
        assertTrue(citation.contains("Ley 18961"))
        assertTrue(citation.contains("Biblioteca del Congreso Nacional"))
    }

    @Test
    fun `apa citation generator formats reglamento correctly`() {
        val citation = generateApaCitation(
            type = "Reglamento",
            author = "",
            year = "2018",
            title = "Disciplina de Carabineros",
            source = "11"
        )

        assertTrue(citation.contains("Carabineros de Chile"))
        assertTrue(citation.contains("(2018)"))
        assertTrue(citation.contains("Reglamento de Disciplina de Carabineros, N° 11"))
    }

    @Test
    fun `calculate urgency identifies normal and urgent dates`() {
        val normalUrgency = calculateUrgency("20 dic 2026")
        assertTrue(normalUrgency == DeliverableUrgency.NORMAL || normalUrgency == DeliverableUrgency.UPCOMING)

        val pastUrgency = calculateUrgency("01 jan 2020")
        assertEquals(DeliverableUrgency.EXPIRED, pastUrgency)
    }
}
