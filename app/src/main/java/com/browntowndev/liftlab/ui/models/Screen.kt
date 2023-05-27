package com.browntowndev.liftlab.ui.models

import androidx.compose.ui.graphics.vector.ImageVector

sealed interface Screen {
    val route: String
    val isAppBarVisible: Boolean
    val navigationIcon: ImageVector?
    val navigationIconContentDescription: String?
    val navigationIconVisible: Boolean?
    val onNavigationIconClick: (() -> Unit)?
    val title: String
    val actions: MutableList<ActionMenuItem>
}