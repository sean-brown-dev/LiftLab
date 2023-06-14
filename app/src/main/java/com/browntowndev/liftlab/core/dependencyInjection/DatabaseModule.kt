package com.browntowndev.liftlab.core.dependencyInjection

import com.browntowndev.liftlab.core.persistence.repositories.RepositoryHelper
import org.koin.dsl.module

val repositoryModule = module {
    factory { RepositoryHelper(get()).lifts }
    factory { RepositoryHelper(get()).workouts }
    factory { RepositoryHelper(get()).programs }
}
