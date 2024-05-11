package com.browntowndev.liftlab.ui.viewmodels.states.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.ui.models.ActionMenuItem
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.models.BottomNavItem
import org.greenrobot.eventbus.EventBus
import org.koin.core.component.inject

data class LiftLibraryScreen(
    override val isOverflowMenuExpanded: Boolean = false,
    override val isOverflowMenuIconVisible: Boolean = true,
    override val navigationIconVisible: Boolean = false,
    override val title: String = navigation.title,
    val filterText: String = "",
    val isSearchBarVisible: Boolean = false,
    val isSearchIconVisible: Boolean = true,
    val isFilterIconVisible: Boolean = true,
    val isConfirmAddLiftVisible: Boolean = false,
    val isCreateNewLiftIconVisible: Boolean = true,
) : BaseScreen() {
    companion object {
        val navigation = BottomNavItem("Lifts", "", R.drawable.list_icon, "liftLibrary?callerRoute={callerRoute}&workoutId={workoutId}&workoutLiftId={workoutLiftId}&movementPattern={movementPattern}&addAtPosition={addAtPosition}")
        const val SEARCH_ICON = "searchIcon"
        const val CREATE_NEW_LIFT_ICON = "createNewLiftIcon"
        const val CONFIRM_ADD_LIFT_ICON = "confirmAddLiftIcon"
        const val LIFT_NAME_FILTER_TEXTVIEW = "liftNameFilterTextView"
        const val LIFT_MOVEMENT_PATTERN_FILTER_ICON = "liftMovementPatternFilterIcon"
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
        return when (controlName) {
            LIFT_NAME_FILTER_TEXTVIEW -> {
                if (isSearchBarVisible != isVisible) {
                    copy(isSearchBarVisible = isVisible)
                }
                else this
            }
            SEARCH_ICON -> {
                copy(isSearchIconVisible = isVisible)
            }
            LIFT_MOVEMENT_PATTERN_FILTER_ICON -> {
                copy(isFilterIconVisible = isVisible)
            }
            CONFIRM_ADD_LIFT_ICON -> {
                copy(isConfirmAddLiftVisible = isVisible)
            }
            CREATE_NEW_LIFT_ICON -> {
                copy(isCreateNewLiftIconVisible = isVisible)
            }
            else -> super.setControlVisibility(controlName, isVisible)
        }
    }

    override fun <T> mutateControlValue(request: AppBarMutateControlRequest<T>): Screen {
        return when (request.controlName) {
            LIFT_NAME_FILTER_TEXTVIEW ->  {
                val newFilter = request.payload as String
                _eventBus.post(TopAppBarEvent.PayloadActionEvent(TopAppBarAction.SearchTextChanged, newFilter))
                copy(filterText = newFilter)
            }
            else -> super.mutateControlValue(request)
        }
    }

    override val route: String
        get() = navigation.route
    override val isAppBarVisible: Boolean
        get() = true
    override val navigationIcon: Either<ImageVector, Int>
        get() = Icons.AutoMirrored.Filled.ArrowBack.left()
    override val navigationIconContentDescription: String?
        get() = null
    override val onNavigationIconClick: (() -> List<Pair<String, Boolean>>)
        get() = {
            _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.NavigatedBack))
            listOf(Pair(LIFT_NAME_FILTER_TEXTVIEW, false))
        }
    override val actions: List<ActionMenuItem> by derivedStateOf {
        listOf(
            ActionMenuItem.IconMenuItem.AlwaysShown(
                controlName = SEARCH_ICON,
                title = "Search",
                icon = Icons.Filled.Search.left(),
                isVisible = !isSearchBarVisible && isSearchIconVisible,
                onClick = {
                    listOf(Pair(LIFT_NAME_FILTER_TEXTVIEW, true))
                },
                contentDescriptionResourceId = R.string.accessibility_search,
            ),
            ActionMenuItem.TextInputMenuItem.AlwaysShown(
                controlName = LIFT_NAME_FILTER_TEXTVIEW,
                icon = Icons.Filled.Search.left(),
                isVisible = isSearchBarVisible,
                value = filterText,
                onValueChange = {
                    _eventBus.post(TopAppBarEvent.PayloadActionEvent(TopAppBarAction.SearchTextChanged, it))
                },
                onClickTrailingIcon = {
                    listOf(Pair(LIFT_NAME_FILTER_TEXTVIEW, false))
                },
            ),
            ActionMenuItem.IconMenuItem.AlwaysShown(
                controlName = LIFT_MOVEMENT_PATTERN_FILTER_ICON,
                title = "Filter",
                isVisible = !isSearchBarVisible && isFilterIconVisible,
                onClick = {
                    _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.FilterStarted))
                    listOf()
                },
                icon = R.drawable.filter_icon.right(),
                contentDescriptionResourceId = R.string.accessibility_filter,
            ),
            ActionMenuItem.IconMenuItem.AlwaysShown(
                controlName = CREATE_NEW_LIFT_ICON,
                title = "Create Lift",
                isVisible = !isSearchBarVisible && isFilterIconVisible && isCreateNewLiftIconVisible,
                onClick = {
                    _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.CreateNewLift))
                    listOf()
                },
                icon = Icons.Filled.Add.left(),
                contentDescriptionResourceId = R.string.create_a_new_lift,
            ),
            ActionMenuItem.IconMenuItem.AlwaysShown(
                controlName = CONFIRM_ADD_LIFT_ICON,
                title = "Confirm Add Lift",
                isVisible = isConfirmAddLiftVisible,
                onClick = {
                    _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.ConfirmAddLift))
                    listOf()
                },
                icon = Icons.Filled.Check.left(),
                contentDescriptionResourceId = null,
            ),
        )
    }
}