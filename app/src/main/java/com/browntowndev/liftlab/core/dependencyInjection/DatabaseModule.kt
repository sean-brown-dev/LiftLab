package com.browntowndev.liftlab.core.dependencyInjection

import com.browntowndev.liftlab.core.data.repositories.LiftsRepository
import com.browntowndev.liftlab.core.data.repositories.Repository
import com.browntowndev.liftlab.core.data.repositories.RepositoryHelper
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {
    single { androidContext() }
    RepositoryHelper().getRepositories().forEach { repo ->
        if (repo is LiftsRepository) {
            single<LiftsRepository> { repo }
        }
    }
}
