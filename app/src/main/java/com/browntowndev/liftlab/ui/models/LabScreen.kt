package com.browntowndev.liftlab.ui.models

import androidx.compose.ui.graphics.vector.ImageVector
import com.browntowndev.liftlab.R

class LabScreen : Screen {
    companion object {
        val navigation = NavItem("Lab", R.drawable.lab_flask, "lab")
    }

    override val route: String
        get() = navigation.route
    override val title: String
        get() = navigation.title
    override val isAppBarVisible: Boolean
        get() = true
    override val navigationIcon: ImageVector?
        get() = null
    override val navigationIconVisible: Boolean?
        get() = false
    override val navigationIconContentDescription: String?
        get() = null
    override val onNavigationIconClick: (() -> Unit)?
        get() = null
    override val actions: MutableList<ActionMenuItem>
        get() = mutableListOf()
}