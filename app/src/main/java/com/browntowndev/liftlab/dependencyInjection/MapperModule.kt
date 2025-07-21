package com.browntowndev.liftlab.dependencyInjection

import com.browntowndev.liftlab.core.domain.mapping.CustomLiftSetMapper
import com.browntowndev.liftlab.core.domain.mapping.ProgramMapper
import com.browntowndev.liftlab.core.domain.mapping.SetResultMapper
import com.browntowndev.liftlab.core.domain.mapping.WorkoutLiftMapper
import com.browntowndev.liftlab.core.domain.mapping.WorkoutLogEntryMapper
import com.browntowndev.liftlab.core.domain.mapping.WorkoutMapper
import com.browntowndev.liftlab.core.domain.progression.StandardProgressionFactory
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