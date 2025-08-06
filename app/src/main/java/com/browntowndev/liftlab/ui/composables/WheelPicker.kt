package com.browntowndev.liftlab.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

@Composable
fun convertTextUnitToDp(textUnit: TextUnit, padding: Dp): Dp {
    return with(LocalDensity.current) {
        textUnit.toDp() + (padding * 2)
    }
}