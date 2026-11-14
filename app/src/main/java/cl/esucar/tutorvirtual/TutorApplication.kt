package cl.esucar.tutorvirtual

import android.app.Application
import cl.esucar.tutorvirtual.di.AppContainer
import cl.esucar.tutorvirtual.di.DefaultAppContainer

class TutorApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
