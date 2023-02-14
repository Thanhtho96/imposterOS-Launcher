package com.tt.imposteroslauncher.di

import com.tt.imposteroslauncher.App
import com.tt.imposteroslauncher.MainViewModel
import com.tt.imposteroslauncher.common.Constant.Dispatcher
import com.tt.imposteroslauncher.data.DataStoreHelper
import com.tt.imposteroslauncher.data.LauncherDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::MainViewModel)
}

val appModule = module {
    singleOf(::DataStoreHelper)
    single { App.appLifeScope }
    single { LauncherDatabase.getDatabase(androidContext()).launcherDao() }
}

val dispatcherModule = module {
    single(named(Dispatcher.MAIN)) { provideMainDispatcher() }
    single(named(Dispatcher.DEFAULT)) { provideDefaultDispatcher() }
    single(named(Dispatcher.IO)) { provideIODispatcher() }
    single(named(Dispatcher.UNCONFINED)) { provideUnconfinedDispatcher() }
}

private fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
private fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
private fun provideIODispatcher(): CoroutineDispatcher = Dispatchers.IO
private fun provideUnconfinedDispatcher(): CoroutineDispatcher = Dispatchers.Unconfined