package com.browntowndev.liftlab.ui.viewmodels.states.topAppBar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.ui.models.ActionMenuItem
import com.browntowndev.liftlab.ui.models.AppBarActionEmission
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.models.NavItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class LiftLibraryScreen(
    val filterText: String = "",
    val isSearchBarVisible: Boolean = false,
    override val isOverflowMenuExpanded: Boolean = false,
) : BaseScreen() {
    companion object {
        val navigation = NavItem("Lifts", R.drawable.list_icon, "liftLibrary")
        const val SEARCH_ICON = "searchIcon"
        const val LIFT_FILTER_TEXTVIEW = "liftFilterTextView"
        const val LIFT_FILTER_VALUE = "liftFilterValue"

        enum class AppBarActions {
            NavigatedBack,
            SearchToggled,
        }

        enum class AppBarPayloadActions {
            FilterTextChanged,
        }
    }

    private var mutableFilterText by mutableStateOf(filterText)

    private val _simpleActionButtons = MutableSharedFlow<AppBarActions>(extraBufferCapacity = 1)
    val simpleActionButtons: Flow<AppBarActions> = _simpleActionButtons.asSharedFlow()

    private val _stringActionButtons = MutableSharedFlow<AppBarActionEmission<AppBarPayloadActions, String>>(extraBufferCapacity = 1)
    val stringActionButtons: Flow<AppBarActionEmission<AppBarPayloadActions, String>> = _stringActionButtons.asSharedFlow()

    override fun toggleControlVisibility(controlName: String): Screen {
        return when (controlName) {
            LIFT_FILTER_TEXTVIEW -> {
                val newScreen = copy(isSearchBarVisible = !this.isSearchBarVisible)
                _stringActionButtons.tryEmit(AppBarActionEmission(AppBarPayloadActions.FilterTextChanged, ""))
                return newScreen
            }
            Screen.OVERFLOW_MENU -> copy(isOverflowMenuExpanded = !this.isOverflowMenuExpanded)
            else -> this
        }
    }

    override fun <T> mutateControlValue(request: AppBarMutateControlRequest<T>): Screen {
        return when (request.controlName) {
            LIFT_FILTER_VALUE -> copy(filterText = request.payload as String)
            else -> this
        }
    }

    override fun copyOverflowMenuToggle(): Screen {
        return copy(isOverflowMenuExpanded = !this.isOverflowMenuExpanded)
    }

    override val route: String
        get() = navigation.route
    override val title: String
        get() = navigation.title
    override val isAppBarVisible: Boolean
        get() = true
    override val navigationIcon: ImageVector?
        get() = Icons.Filled.ArrowBack
    override val navigationIconVisible: Boolean?
        get() = isSearchBarVisible
    override val navigationIconContentDescription: String?
        get() = null
    override val onNavigationIconClick: (() -> Unit)?
        get() = {
            _simpleActionButtons.tryEmit(AppBarActions.NavigatedBack)
        }
    override val actions: List<ActionMenuItem> by derivedStateOf {
        listOf(
            ActionMenuItem.IconMenuItem.AlwaysShown(
                controlName = SEARCH_ICON,
                title = "Search",
                isVisible = !isSearchBarVisible,
                onClick = {
                    _simpleActionButtons.tryEmit(AppBarActions.SearchToggled)
                },
                icon = Icons.Filled.Search,
                contentDescriptionResourceId = R.string.accessibility_search,
            ), ActionMenuItem.TextInputMenuItem.AlwaysShown(
                controlName = LIFT_FILTER_TEXTVIEW,
                icon = Icons.Filled.Search,
                isVisible = isSearchBarVisible,
                value = mutableFilterText,
                onValueChange = {
                    mutableFilterText = it
                    _stringActionButtons.tryEmit(AppBarActionEmission(action = AppBarPayloadActions.FilterTextChanged, value = it))
                },
                onClickTrailingIcon = {
                    _simpleActionButtons.tryEmit(AppBarActions.SearchToggled)
                },
            )
        )
    }
}