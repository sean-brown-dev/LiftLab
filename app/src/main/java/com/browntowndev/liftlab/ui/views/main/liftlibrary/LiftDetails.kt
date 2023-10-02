package com.browntowndev.liftlab.ui.views.main.liftlibrary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import com.browntowndev.liftlab.ui.viewmodels.LiftDetailsViewModel
import com.browntowndev.liftlab.ui.views.composables.FocusableRoundTextField
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LiftDetails(
    liftId: Long,
) {
    val liftDetailsViewModel: LiftDetailsViewModel = koinViewModel { parametersOf(liftId) }
    val state by liftDetailsViewModel.state.collectAsState()

    if (state.lift != null) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FocusableRoundTextField(
                value = state.lift!!.name,
                focus = false,
                onTextFieldValueChange = {
                    liftDetailsViewModel.updateName(it.text)
                }
            )
        }
    }
}