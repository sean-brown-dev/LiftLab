package com.browntowndev.liftlab.core.dependencyInjection

import com.browntowndev.liftlab.core.persistence.mapping.CustomLiftSetMapper
import com.browntowndev.liftlab.core.persistence.mapping.ProgramMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutLiftMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutMapper
import org.koin.dsl.module

val mapperModule = module {
    factory { CustomLiftSetMapper() }
    factory { WorkoutLiftMapper(get()) }
    factory { WorkoutMapper(get()) }
    factory { ProgramMapper(get()) }
}