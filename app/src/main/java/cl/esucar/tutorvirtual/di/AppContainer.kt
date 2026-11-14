package cl.esucar.tutorvirtual.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cl.esucar.tutorvirtual.data.media.Media3RadioGateway
import cl.esucar.tutorvirtual.data.repository.SqliteTutorRepository
import cl.esucar.tutorvirtual.domain.repository.AcademicRepository
import cl.esucar.tutorvirtual.domain.repository.AuthRepository
import cl.esucar.tutorvirtual.domain.repository.RadioPlayerGateway
import cl.esucar.tutorvirtual.domain.usecase.CalculateProgressUseCase
import cl.esucar.tutorvirtual.ui.viewmodel.AuthViewModel
import cl.esucar.tutorvirtual.ui.viewmodel.TutorMainViewModel

interface AppContainer {
    val authRepository: AuthRepository
    val academicRepository: AcademicRepository
    val radioPlayerGateway: RadioPlayerGateway
    val calculateProgressUseCase: CalculateProgressUseCase
    fun authViewModelFactory(): ViewModelProvider.Factory
    fun mainViewModelFactory(): ViewModelProvider.Factory
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext

    private val sqliteRepository: SqliteTutorRepository by lazy {
        SqliteTutorRepository(appContext)
    }

    override val authRepository: AuthRepository
        get() = sqliteRepository

    override val academicRepository: AcademicRepository
        get() = sqliteRepository

    override val radioPlayerGateway: RadioPlayerGateway by lazy {
        Media3RadioGateway(appContext)
    }

    override val calculateProgressUseCase: CalculateProgressUseCase by lazy {
        CalculateProgressUseCase()
    }

    override fun authViewModelFactory(): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                    return AuthViewModel(authRepository = authRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }

    override fun mainViewModelFactory(): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TutorMainViewModel::class.java)) {
                    return TutorMainViewModel(
                        authRepository = authRepository,
                        academicRepository = academicRepository,
                        radioPlayerGateway = radioPlayerGateway,
                        calculateProgressUseCase = calculateProgressUseCase
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
}
