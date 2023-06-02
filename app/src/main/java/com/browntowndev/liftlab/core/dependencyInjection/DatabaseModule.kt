package com.browntowndev.liftlab.core.dependencyInjection

import com.browntowndev.liftlab.core.data.repositories.LiftsRepository
import com.browntowndev.liftlab.core.data.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.data.repositories.RepositoryHelper
import com.browntowndev.liftlab.core.data.repositories.WorkoutsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {
    single { androidContext() }
    RepositoryHelper().getRepositories().forEach { repo ->
        when(repo) {
            is LiftsRepository -> single { repo }
            is ProgramsRepository -> single { repo }
            is WorkoutsRepository -> single { repo }
        }
    }
}
