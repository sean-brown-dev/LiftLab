package com.browntowndev.liftlab.ui.views.main.workout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import arrow.core.Either
import com.browntowndev.liftlab.core.persistence.dtos.EditWorkoutMetadataDto
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.EditWorkoutViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EditWorkout(
    workoutId: Long,
    mesoCycle: Int,
    microCycle: Int,
    paddingValues: PaddingValues,
    navHostController: NavHostController,
    mutateTopAppBarControlValue: (AppBarMutateControlRequest<Either<String?, Triple<Long, Long, Boolean>>>) -> Unit,
    setTopAppBarCollapsed: (Boolean) -> Unit,
    setBottomNavBarVisibility: (visible: Boolean) -> Unit,
) {
    val editingWorkoutMeta = EditWorkoutMetadataDto(
        workoutLogEntryId = workoutId,
        mesoCycle = mesoCycle,
        microCycle = microCycle,
    )
    val editWorkoutViewModel: EditWorkoutViewModel = koinViewModel {
        parametersOf({ editingWorkoutMeta })
    }
    val state by editWorkoutViewModel.state.collectAsState()
}