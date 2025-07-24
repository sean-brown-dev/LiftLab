package com.browntowndev.liftlab

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.browntowndev.liftlab.dependencyInjection.eventBusModule
import com.browntowndev.liftlab.dependencyInjection.syncModule
import com.browntowndev.liftlab.dependencyInjection.useCaseModule
import com.browntowndev.liftlab.dependencyInjection.repositoryModule
import com.browntowndev.liftlab.dependencyInjection.liftLabScopeModule
import com.browntowndev.liftlab.dependencyInjection.notificationModule
import com.browntowndev.liftlab.dependencyInjection.viewModelModule
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
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin

class LiftLabApp() : Application() {
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
            workManagerFactory()
            modules(
                liftLabScopeModule,
                useCaseModule,
                repositoryModule,
                syncModule,
                viewModelModule,
                eventBusModule,
                notificationModule
            )
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
