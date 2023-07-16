package imposter.os.launcher

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import imposter.os.launcher.di.appModule
import imposter.os.launcher.di.dispatcherModule
import imposter.os.launcher.di.viewModelModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        mInstance = this

        startKoin {
            androidContext(this@App)
            modules(appModule, viewModelModule, dispatcherModule)
        }
    }

    companion object {
        // At the top level of your kotlin file:
        val Context.dataStore by preferencesDataStore(name = "settings")

        val appLifeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        private var mInstance: App? = null
        val instance get() = mInstance!!
    }
}