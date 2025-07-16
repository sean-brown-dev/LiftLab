package com.browntowndev.liftlab

import android.app.Application
import android.util.Log
import com.browntowndev.liftlab.core.dependencyInjection.eventBusModule
import com.browntowndev.liftlab.core.dependencyInjection.firebaseModule
import com.browntowndev.liftlab.core.dependencyInjection.mapperModule
import com.browntowndev.liftlab.core.dependencyInjection.repositoryModule
import com.browntowndev.liftlab.core.dependencyInjection.liftLabScopeModule
import com.browntowndev.liftlab.core.dependencyInjection.viewModelModule
import com.google.firebase.Firebase
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize
import com.mmk.kmpauth.google.GoogleAuthCredentials
import com.mmk.kmpauth.google.GoogleAuthProvider
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class LiftLabApp : Application() {
    override fun onCreate() {
        super.onCreate()

        Firebase.initialize(context = this)
        Firebase.appCheck.installAppCheckProviderFactory(
            getAppCheckProviderFactory(),
        )

        GoogleAuthProvider.create(credentials = GoogleAuthCredentials(serverId = getString(R.string.firebase_client_id)))

        startKoin {
            androidLogger()
            androidContext(this@LiftLabApp)
            modules(liftLabScopeModule, mapperModule, repositoryModule, firebaseModule, viewModelModule, eventBusModule)
        }
    }

    fun getAppCheckProviderFactory(): AppCheckProviderFactory {
        return if (BuildConfig.LOCAL_DEV_BUILD) {
            Log.w("AppCheck", "Using DebugAppCheckProvider for local development")
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
    }
}
