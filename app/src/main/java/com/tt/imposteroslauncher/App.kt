package com.tt.imposteroslauncher

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.tt.imposteroslauncher.di.appModule
import com.tt.imposteroslauncher.di.dispatcherModule
import com.tt.imposteroslauncher.di.viewModelModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        mInstance = this
        mAppLifeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        startKoin {
            androidContext(this@App)
            modules(appModule, viewModelModule, dispatcherModule)
        }
    }

    companion object {
        // At the top level of your kotlin file:
        val Context.dataStore by preferencesDataStore(name = "settings")

        private var mAppLifeScope: CoroutineScope? = null
        val appLifeScope get() = mAppLifeScope!!

        private var mInstance: App? = null
        val instance get() = mInstance!!
    }
}