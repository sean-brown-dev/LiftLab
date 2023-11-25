package com.browntowndev.liftlab.core.dependencyInjection

import com.browntowndev.liftlab.core.persistence.mapping.CustomLiftSetMapper
import com.browntowndev.liftlab.core.persistence.mapping.ProgramMapper
import com.browntowndev.liftlab.core.persistence.mapping.SetResultMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutLiftMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutLogEntryMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutMapper
import com.browntowndev.liftlab.core.progression.StandardProgressionFactory
import org.koin.dsl.module

val mapperModule = module {
    factory { CustomLiftSetMapper() }
    factory { WorkoutLiftMapper(get()) }
    factory { WorkoutMapper(get()) }
    factory { ProgramMapper(get()) }
    factory { SetResultMapper() }
    factory { WorkoutLogEntryMapper() }
    factory { StandardProgressionFactory() }
}