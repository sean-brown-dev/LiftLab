package com.browntowndev.liftlab

import android.app.Application
import com.browntowndev.liftlab.core.dependencyInjection.repositoryModule
import com.browntowndev.liftlab.core.dependencyInjection.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class LiftLabApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@LiftLabApp)
            modules(repositoryModule, viewModelModule)
        }
    }
}
