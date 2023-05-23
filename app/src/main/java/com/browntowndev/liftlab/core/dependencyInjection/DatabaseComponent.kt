package com.browntowndev.liftlab.core.dependencyInjection

import com.browntowndev.liftlab.core.data.repositories.LiftsRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DatabaseComponent : KoinComponent {
    private val liftsRepository: LiftsRepository by inject()
}