package com.browntowndev.liftlab.ui.models

open class NavItem(open val title: String, open val subtitle: String = "", open val route: String)
class BottomNavItem(
    override val title: String,
    override val subtitle: String = "",
    val bottomNavIconResourceId: Int,
    override val route: String
):  NavItem(title, subtitle, route)