package com.browntowndev.liftlab.ui.viewmodels.states.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.ui.models.ActionMenuItem
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.models.BottomNavItem
import org.greenrobot.eventbus.EventBus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class LiftLibraryScreen(
    override val isOverflowMenuExpanded: Boolean = false,
    override val isOverflowMenuIconVisible: Boolean = false,
    override val navigationIconVisible: Boolean = false,
    override val title: String = navigation.title,
    val filterText: String = "",
) : BaseScreen(), KoinComponent {
    companion object {
        val navigation = BottomNavItem("Lifts", "", R.drawable.list_icon, "liftLibrary")
        const val SEARCH_ICON = "searchIcon"
        const val LIFT_NAME_FILTER_TEXTVIEW = "liftNameFilterTextView"
        const val LIFT_MOVEMENT_PATTERN_FILTER_ICON = "liftMovementPatternFilterIcon"
    }

    private var mutableFilterText by mutableStateOf(filterText)
    var isSearchBarVisible by mutableStateOf(false)
    private var isSearchIconVisible by mutableStateOf(true)
    private var isFilterIconVisible by mutableStateOf(true)

    private val _eventBus: EventBus by inject()
    
    override fun copySetOverflowIconVisibility(isVisible: Boolean): Screen {
        return if (isVisible != this.isOverflowMenuIconVisible) copy(isOverflowMenuIconVisible = isVisible, filterText = mutableFilterText) else this
    }

    override fun copySetOverflowMenuVisibility(isVisible: Boolean): Screen {
        return if(isOverflowMenuExpanded != isVisible) copy(isOverflowMenuExpanded = isVisible, filterText = mutableFilterText) else this
    }

    override fun copySetNavigationIconVisibility(isVisible: Boolean): Screen {
        return if(navigationIconVisible != isVisible) copy(navigationIconVisible = !this.navigationIconVisible, filterText = mutableFilterText) else this
    }

    override fun copyTitleMutation(newTitle: String): Screen {
        return if (newTitle != title) copy(title = newTitle, filterText = mutableFilterText) else this
    }

    override fun setControlVisibility(controlName: String, isVisible: Boolean): Screen {
        val superCopy = super.setControlVisibility(controlName, isVisible)
        return when (controlName) {
            LIFT_NAME_FILTER_TEXTVIEW -> {
                if (isSearchBarVisible != isVisible) {
                    isSearchBarVisible = isVisible
                }
                this
            }
            SEARCH_ICON -> {
                isSearchIconVisible = isVisible
                this
            }
            LIFT_MOVEMENT_PATTERN_FILTER_ICON -> {
                isFilterIconVisible = isVisible
                this
            }
            else -> superCopy
        }
    }

    override fun <T> mutateControlValue(request: AppBarMutateControlRequest<T>): Screen {
        val superCopy = super.mutateControlValue(request)
        return when (request.controlName) {
            LIFT_NAME_FILTER_TEXTVIEW ->  {
                val newFilter = request.payload as String
                mutableFilterText = newFilter
                _eventBus.post(TopAppBarEvent.PayloadActionEvent(TopAppBarAction.SearchTextChanged, newFilter))
                return this
            }
            else -> superCopy
        }
    }

    override val route: String
        get() = navigation.route
    override val isAppBarVisible: Boolean
        get() = true
    override val navigationIcon: ImageVector?
        get() = Icons.Filled.ArrowBack
    override val navigationIconContentDescription: String?
        get() = null
    override val onNavigationIconClick: (() -> Unit)?
        get() = {
            isSearchBarVisible = false
            _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.NavigatedBack))
        }
    override val actions: List<ActionMenuItem> by derivedStateOf {
        listOf(
            ActionMenuItem.IconMenuItem.AlwaysShown(
                controlName = SEARCH_ICON,
                title = "Search",
                isVisible = !isSearchBarVisible && isSearchIconVisible,
                onClick = {
                    isSearchBarVisible = true
                    _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.SearchStarted))
                },
                icon = Icons.Filled.Search,
                contentDescriptionResourceId = R.string.accessibility_search,
            ), ActionMenuItem.TextInputMenuItem.AlwaysShown(
                controlName = LIFT_NAME_FILTER_TEXTVIEW,
                icon = Icons.Filled.Search,
                isVisible = isSearchBarVisible,
                value = mutableFilterText,
                onValueChange = {
                    mutableFilterText = it
                    _eventBus.post(TopAppBarEvent.PayloadActionEvent(TopAppBarAction.SearchTextChanged, it))
                },
                onClickTrailingIcon = {
                    isSearchBarVisible = false
                    mutableFilterText = ""
                    _eventBus.post(TopAppBarEvent.PayloadActionEvent(TopAppBarAction.SearchTextChanged, mutableFilterText))
                },
            ),
            ActionMenuItem.IconMenuItem.AlwaysShown(
                controlName = LIFT_MOVEMENT_PATTERN_FILTER_ICON,
                title = "Filter",
                isVisible = !isSearchBarVisible && isFilterIconVisible,
                onClick = {
                    _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.FilterStarted))
                },
                iconPainterResourceId = R.drawable.filter_icon,
                contentDescriptionResourceId = R.string.accessibility_search,
            ),
        )
    }
}