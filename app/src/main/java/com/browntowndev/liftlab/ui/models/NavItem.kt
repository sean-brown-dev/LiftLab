package com.browntowndev.liftlab.ui.models

import com.browntowndev.liftlab.ui.views.navigation.Route

open class NavItem(open val title: String, open val subtitle: String = "", open val route: Route)
class BottomNavItem(
    override val title: String,
    override val subtitle: String = "",
    val bottomNavIconResourceId: Int,
    override val route: Route
):  NavItem(title, subtitle, route)