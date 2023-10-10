package com.browntowndev.liftlab.ui.viewmodels.states.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import arrow.core.Either
import arrow.core.left
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.ui.models.ActionMenuItem
import com.browntowndev.liftlab.ui.models.NavItem
import org.greenrobot.eventbus.EventBus
import org.koin.core.component.inject

data class LiftDetailsScreen(
    override val isOverflowMenuExpanded: Boolean = false,
    override val isOverflowMenuIconVisible: Boolean = false,
    override val navigationIconVisible: Boolean = true,
    override val title: String = navigation.title,
    private val isConfirmCreateLiftVisible: Boolean = false,
): BaseScreen() {
    companion object {
        val navigation = NavItem("Lift Metrics", "", "liftDetails/{id}")
        const val CONFIRM_CREATE_LIFT_ICON = "confirmCreateLiftIcon"
    }

    private val _eventBus: EventBus by inject()

    override fun copySetOverflowIconVisibility(isVisible: Boolean): Screen {
        return if (isVisible != this.isOverflowMenuIconVisible) copy(isOverflowMenuIconVisible = isVisible) else this
    }

    override fun copySetOverflowMenuVisibility(isVisible: Boolean): Screen {
        return if (isVisible != this.isOverflowMenuExpanded) copy(isOverflowMenuExpanded = !this.isOverflowMenuExpanded) else this
    }

    override fun copySetNavigationIconVisibility(isVisible: Boolean): Screen {
        return if (isVisible != navigationIconVisible) copy(navigationIconVisible = !this.navigationIconVisible) else this
    }

    override fun copyTitleMutation(newTitle: String): Screen {
        return if (title != newTitle) copy(title = newTitle) else this
    }

    override fun setControlVisibility(controlName: String, isVisible: Boolean): Screen {
        val superCopy = super.setControlVisibility(controlName, isVisible)
        return when (controlName) {
            CONFIRM_CREATE_LIFT_ICON -> {
                copy(isConfirmCreateLiftVisible = isVisible)
            }
            else -> superCopy
        }
    }

    override val route: String
        get() = navigation.route
    override val isAppBarVisible: Boolean
        get() = true
    override val navigationIcon: Either<ImageVector, Int>?
        get() = Icons.Filled.ArrowBack.left()
    override val navigationIconContentDescription: String?
        get() = null
    override val onNavigationIconClick: (() -> Unit)?
        get() = { _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.NavigatedBack)) }
    override val actions: List<ActionMenuItem> by derivedStateOf {
        listOf(
            ActionMenuItem.IconMenuItem.AlwaysShown(
                controlName = CONFIRM_CREATE_LIFT_ICON,
                title = "Confirm Create Lift",
                isVisible = isConfirmCreateLiftVisible,
                onClick = {
                    _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.ConfirmCreateNewLift))
                },
                icon = Icons.Filled.Check.left(),
                contentDescriptionResourceId = null,
            ),
        )
    }
}