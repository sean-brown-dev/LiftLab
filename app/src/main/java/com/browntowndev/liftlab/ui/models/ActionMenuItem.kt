package com.browntowndev.liftlab.ui.models

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.TextFieldValue

sealed interface ActionMenuItem {
    val isVisible: Boolean
    val icon: ImageVector

    sealed interface IconMenuItem : ActionMenuItem {
        val title: String
        val onClick: () -> Unit
        val contentDescriptionResourceId: Int

        data class AlwaysShown  (
            override val title: String,
            override val isVisible: Boolean,
            override val contentDescriptionResourceId: Int,
            override val onClick: () -> Unit,
            override val icon: ImageVector,
        ) : IconMenuItem

        data class ShownIfRoom(
            override val title: String,
            override val isVisible: Boolean,
            override val contentDescriptionResourceId: Int,
            override val onClick: () -> Unit,
            override val icon: ImageVector,
        ) : IconMenuItem

        data class NeverShown(
            override val title: String,
            override val isVisible: Boolean,
            override val contentDescriptionResourceId: Int,
            override val onClick: () -> Unit,
            override val icon: ImageVector,
        ) : IconMenuItem
    }

    sealed interface TextInputMenuItem : ActionMenuItem {
        val value: String
        override val isVisible: Boolean
        val onValueChange: (String) -> Unit
        val onClickTrailingIcon: () -> Unit

        data class AlwaysShown(
            override val icon: ImageVector,
            override val value: String,
            override val isVisible: Boolean,
            override val onValueChange: (String) -> Unit,
            override val onClickTrailingIcon: () -> Unit,
        ) : TextInputMenuItem
    }
}