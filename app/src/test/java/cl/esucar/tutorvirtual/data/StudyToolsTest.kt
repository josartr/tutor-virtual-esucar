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
