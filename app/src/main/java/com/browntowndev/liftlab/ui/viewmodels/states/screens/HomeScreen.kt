package com.browntowndev.liftlab.ui.viewmodels.states.screens

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import arrow.core.Either
import arrow.core.right
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.domain.enums.TopAppBarAction
import com.browntowndev.liftlab.ui.models.controls.TopAppBarEvent
import com.browntowndev.liftlab.ui.models.controls.ActionMenuItem
import com.browntowndev.liftlab.ui.models.controls.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.models.controls.BottomNavItem
import com.browntowndev.liftlab.ui.models.controls.Route
import org.greenrobot.eventbus.EventBus
import org.koin.core.component.inject

data class HomeScreen(
    override val isOverflowMenuExpanded: Boolean = false,
    override val isOverflowMenuIconVisible: Boolean = true,
    override val navigationIconVisible: Boolean = false,
    override val title: String = navigation.title,
    private val isSyncEnabled: Boolean = false,
) : BaseScreen() {
    companion object {
        private const val SETTINGS = "Settings"
        const val SYNC_STATUS = "Upsert Status"
        val navigation = BottomNavItem("Home", "", R.drawable.home_icon, Route.Home)
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

    override fun <T> mutateControlValue(request: AppBarMutateControlRequest<T>): Screen {
        return when (request.controlName) {
            SYNC_STATUS -> {
                if (request.payload !is Boolean) throw IllegalArgumentException("Payload must be of type Boolean")
                copy(isSyncEnabled = request.payload)
            }
            else -> super.mutateControlValue(request)
        }
    }

    private val syncIcon: Either<ImageVector, Int>
        get() = (if (isSyncEnabled) R.drawable.cloud_done else R.drawable.cloud_off).right()
    private val syncIconColor: Color?
        get() = if (!isSyncEnabled) Color.LightGray else null
    override val route: Route
        get() = navigation.route
    override val isAppBarVisible: Boolean
        get() = true
    override val navigationIcon: Either<ImageVector, Int>?
        get() = null
    override val navigationIconContentDescription: String?
        get() = null
    override val onNavigationIconClick: (() -> List<Pair<String, Boolean>>)?
        get() = null
    override val actions: List<ActionMenuItem> by derivedStateOf {
        listOf(
            ActionMenuItem.IconMenuItem.AlwaysShown(
                controlName = SYNC_STATUS,
                title = "Upsert Status",
                icon = syncIcon,
                color = syncIconColor,
                isVisible = true,
                placeAtStart = true,
                onClick = {
                    _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.OpenProfileMenu))
                    listOf()
                },
                contentDescriptionResourceId = R.string.profile,
            ),
            ActionMenuItem.IconMenuItem.AlwaysShown(
                controlName = SETTINGS,
                title = "Settings",
                icon = R.drawable.settings_cog.right(),
                isVisible = true,
                onClick = {
                    _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.OpenSettingsMenu))
                    listOf()
                },
                contentDescriptionResourceId = R.string.settings,
            ),
        )
    }
}