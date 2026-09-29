package lamz.netblocker

import android.app.Application
import lamz.netblocker.di.AppContainer

class NetBlockerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
