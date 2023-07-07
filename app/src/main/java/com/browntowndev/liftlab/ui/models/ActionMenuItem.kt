package com.browntowndev.liftlab.ui.models

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import arrow.core.Either

sealed interface ActionMenuItem {
    val controlName: String
    val isVisible: Boolean
    val icon: Either<ImageVector, Int>?
    val trailingIconText: String?

    sealed interface IconMenuItem : ActionMenuItem {
        val title: String
        val dividerBelow: Boolean
        val onClick: () -> Unit
        val contentDescriptionResourceId: Int?

        data class AlwaysShown  (
            override val controlName: String,
            override val title: String,
            override val isVisible: Boolean,
            override val contentDescriptionResourceId: Int? = null,
            override val onClick: () -> Unit,
            override val icon: Either<ImageVector, Int>,
            override val trailingIconText: String? = null,
            override val dividerBelow: Boolean = false,
        ) : IconMenuItem

        data class ShownIfRoom(
            override val controlName: String,
            override val title: String,
            override val isVisible: Boolean,
            override val contentDescriptionResourceId: Int? = null,
            override val onClick: () -> Unit,
            override val icon: Either<ImageVector, Int>,
            override val trailingIconText: String? = null,
            override val dividerBelow: Boolean = false,
        ) : IconMenuItem

        data class NeverShown(
            override val controlName: String,
            override val title: String,
            override val isVisible: Boolean,
            override val contentDescriptionResourceId: Int? = null,
            override val onClick: () -> Unit,
            override val icon: Either<ImageVector, Int>,
            override val trailingIconText: String? = null,
            override val dividerBelow: Boolean = false,
        ) : IconMenuItem
    }

    sealed interface TimerMenuItem : ActionMenuItem {
        override val isVisible: Boolean

        data class AlwaysShown(
            override val controlName: String,
            override val icon: Either<ImageVector, Int>,
            override val trailingIconText: String? = null,
            override val isVisible: Boolean = false,
            val started: Boolean = false,
            val startTimeInMillis: Long = 0,
        ) : TimerMenuItem
    }

    sealed interface TextInputMenuItem : ActionMenuItem {
        val value: String
        override val isVisible: Boolean
        val onValueChange: (String) -> Unit
        val onClickTrailingIcon: () -> Unit

        data class AlwaysShown(
            override val controlName: String,
            override val icon: Either<ImageVector, Int>,
            override val trailingIconText: String? = null,
            override val value: String,
            override val isVisible: Boolean,
            override val onValueChange: (String) -> Unit,
            override val onClickTrailingIcon: () -> Unit,
        ) : TextInputMenuItem
    }

    sealed interface ButtonMenuItem : ActionMenuItem {
        override val isVisible: Boolean
        val onClick: () -> Unit

        data class AlwaysShown(
            override val controlName: String,
            val buttonContent: @Composable RowScope.() -> Unit,
            override val icon: Either<ImageVector, Int>? = null,
            override val trailingIconText: String? = null,
            override val isVisible: Boolean = true,
            override val onClick: () -> Unit,
        ): ButtonMenuItem
    }
}