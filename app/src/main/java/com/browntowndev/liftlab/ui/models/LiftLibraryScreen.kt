package com.browntowndev.liftlab.ui.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.enums.AppBarIconActions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class LiftLibraryScreen : Screen {
    companion object {
        val navigation = NavItem("Lifts", R.drawable.list_icon, "liftLibrary")
    }

    var filterText = mutableStateOf("")
    private var isSearchBarVisible by mutableStateOf(false)
    private val _buttons = MutableSharedFlow<AppBarIconActions>(extraBufferCapacity = 1)
    val buttons: Flow<AppBarIconActions> = _buttons.asSharedFlow()

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
            filterText.value = ""
            isSearchBarVisible = false
        }
    override val actions: MutableList<ActionMenuItem>
        get() = mutableListOf(
            ActionMenuItem.IconMenuItem.AlwaysShown(
                title = "Search",
                isVisible = !isSearchBarVisible,
                onClick = {
                    isSearchBarVisible = true
                },
                icon = Icons.Filled.Search,
                contentDescriptionResourceId = R.string.accessibility_search,
            ), ActionMenuItem.TextInputMenuItem.AlwaysShown(
                icon= Icons.Filled.Search,
                isVisible = isSearchBarVisible,
                value = filterText.value,
                onValueChange = {
                    filterText.value = it
                },
                onClickTrailingIcon = {
                    isSearchBarVisible = false
                },
            )
        )
}