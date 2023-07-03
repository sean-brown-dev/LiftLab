package com.browntowndev.liftlab.ui.views.main.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.common.insertSuperscript
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.WorkoutViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import com.browntowndev.liftlab.ui.views.utils.CircledTextIcon
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Workout(
    paddingValues: PaddingValues,
    setTopAppBarCollapsed: (Boolean) -> Unit,
    mutateTopAppBarControlValue: (AppBarMutateControlRequest<String?>) -> Unit,
    setBottomSheetContent: (label: String, volumeChipLabels: List<CharSequence>) -> Unit,
) {
    val workoutViewModel: WorkoutViewModel = koinViewModel()
    val state by workoutViewModel.state.collectAsState()

    LaunchedEffect(state.workout?.id) {
        if (state.workout != null) {
            mutateTopAppBarControlValue(
                AppBarMutateControlRequest(
                    Screen.TITLE,
                    state.workout!!.name
                )
            )
            mutateTopAppBarControlValue(
                AppBarMutateControlRequest(
                    Screen.SUBTITLE,
                    "Mesocycle ${state.program!!.currentMesocycle + 1}"
                )
            )
            setBottomSheetContent("Workout Volume", state.volumeTypes)
        }
    }

    LazyColumn(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
    ) {
        item {
            Spacer(modifier = Modifier.height(5.dp))
        }
        items(state.workout?.lifts ?: listOf()) { lift ->
            val movementPatternDisplayName = remember { lift.liftMovementPattern.displayName() }
            val liftFirstLetter = remember { lift.liftName[0].toString() }
            val hasMyoRepSets =
                remember { (lift as? CustomWorkoutLiftDto)?.customLiftSets?.fastAny { it is MyoRepSetDto } }
                    ?: false
            val liftNameWithSetCount = remember {
                val setCountAndName = "${lift.setCount} x ${lift.liftName}"
                if (hasMyoRepSets) setCountAndName.insertSuperscript(
                    "+myo",
                    lift.setCount.toString().length - 1
                )
                else setCountAndName
            }
            ListItem(
                headlineContent = { Text(movementPatternDisplayName, fontSize = 20.sp) },
                supportingContent = {
                    if (liftNameWithSetCount is AnnotatedString)
                        Text(liftNameWithSetCount, fontSize = 15.sp)
                    else if (liftNameWithSetCount is String)
                        Text(liftNameWithSetCount, fontSize = 15.sp)
                },
                leadingContent = { CircledTextIcon(text = liftFirstLetter) },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.background,
                    headlineColor = MaterialTheme.colorScheme.onBackground,
                    supportingColor = MaterialTheme.colorScheme.onBackground,
                    leadingIconColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp),
                shape = RoundedCornerShape(5.dp),
                enabled = !state.inProgress,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surface
                ),
                onClick = {
                    workoutViewModel.setInProgress(true)
                }
            ) {
                var text by remember {
                    mutableStateOf(
                        if (!state.inProgress) "Start ${state.workout?.name}"
                        else "In Progress"
                    )
                }

                val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                var textColor by remember {
                    mutableStateOf(
                        if (!state.inProgress) onPrimaryColor
                        else onSurfaceColor
                    )
                }

                LaunchedEffect(key1 = state.inProgress) {
                    textColor = if (!state.inProgress) onPrimaryColor
                    else onSurfaceColor

                    text = if (!state.inProgress) "Start ${state.workout?.name}"
                    else "In Progress"
                }

                Text(
                    text = text,
                    color = textColor,
                )
            }
        }
    }
}

@Composable
fun WorkoutLog(
    paddingValues: PaddingValues,
    lifts: List<GenericWorkoutLift>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
    ) {
        items(lifts, key = { it.id }) {lift ->
            Text(lift.liftName, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
