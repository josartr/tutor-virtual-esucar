package cl.esucar.tutorvirtual.domain.repository

import cl.esucar.tutorvirtual.domain.model.AuthResult
import cl.esucar.tutorvirtual.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentProfile: StateFlow<UserProfile>
    fun loadUserProfile(): UserProfile
    fun registerUser(profile: UserProfile, password: String): AuthResult
    fun login(emailInput: String, password: String): AuthResult
    fun loginQuickSso(provider: String): UserProfile
    fun saveUserProfile(profile: UserProfile)
    fun clearSession()
}
