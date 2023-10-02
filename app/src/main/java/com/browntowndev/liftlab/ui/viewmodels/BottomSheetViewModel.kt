package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.ViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.LiftLabBottomSheetState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalMaterial3Api::class)
class BottomSheetViewModel: ViewModel() {
    private var _state = MutableStateFlow(LiftLabBottomSheetState())
    val state = _state.asStateFlow()

    fun updateSheetContent(
        label: String,
        volumeChipLabels: List<CharSequence>,
    ) {
        _state.update {
            it.copy(
                label = label,
                volumeChipLabels = volumeChipLabels,
            )
        }
    }

    fun setSheetVisibility(visible: Boolean) {
        _state.update {
            it.copy(visible = visible)
        }
    }
}