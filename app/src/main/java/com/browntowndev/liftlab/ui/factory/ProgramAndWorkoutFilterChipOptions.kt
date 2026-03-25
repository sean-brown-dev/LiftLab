package com.browntowndev.liftlab.ui.factory

import com.browntowndev.liftlab.ui.models.controls.FilterChipOption
import com.browntowndev.liftlab.ui.models.controls.FilterChipOption.Companion.PROGRAM
import com.browntowndev.liftlab.ui.models.controls.FilterChipOption.Companion.WORKOUT
import com.browntowndev.liftlab.ui.models.controls.FlowRowFilterChipSection

fun createProgramAndWorkoutFilterChipOptions(
    programNamesById: Map<Long, String>,
    workoutNamesById: Map<Long, String>,
): List<FlowRowFilterChipSection> {
    return listOf(
        object : FlowRowFilterChipSection {
            override val sectionName: String
                get() = "Programs"
            override val filterChipOptions: Lazy<List<FilterChipOption>>
                get() = lazy {
                    programNamesById.map { program ->
                        FilterChipOption(
                            type = PROGRAM,
                            value = program.value,
                            key = program.key
                        )
                    }
                }
        },
        object : FlowRowFilterChipSection {
            override val sectionName: String
                get() = "Workouts"
            override val filterChipOptions: Lazy<List<FilterChipOption>>
                get() = lazy {
                    workoutNamesById.map { workout ->
                        FilterChipOption(
                            type = WORKOUT,
                            value = workout.value,
                            key = workout.key
                        )
                    }
                }
        },
    )
}