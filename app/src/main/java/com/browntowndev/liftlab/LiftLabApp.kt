package com.browntowndev.liftlab

import android.app.Application
import android.content.Context
import com.browntowndev.liftlab.core.dependencyInjection.eventBusModule
import com.browntowndev.liftlab.core.dependencyInjection.firebaseModule
import com.browntowndev.liftlab.core.dependencyInjection.mapperModule
import com.browntowndev.liftlab.core.dependencyInjection.repositoryModule
import com.browntowndev.liftlab.core.dependencyInjection.viewModelModule
import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.mmk.kmpauth.google.GoogleAuthCredentials
import com.mmk.kmpauth.google.GoogleAuthProvider
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class LiftLabApp : Application() {
    override fun onCreate() {
        super.onCreate()
        GoogleAuthProvider.create(credentials = GoogleAuthCredentials(serverId = getString(R.string.firebase_client_id)))

        startKoin {
            androidLogger()
            androidContext(this@LiftLabApp)
            modules(mapperModule, repositoryModule, firebaseModule, viewModelModule, eventBusModule)
        }
    }
}
