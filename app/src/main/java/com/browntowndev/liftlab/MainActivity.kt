package com.browntowndev.liftlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.Text
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.browntowndev.liftlab.ui.views.LiftLab

@ExperimentalFoundationApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingsManager.initialize(super.getApplicationContext())
        LiftLabDatabase.getInstance(super.getApplicationContext())

        setContent {
            if(LiftLabDatabase.initialized) {
                LiftLab()
            } else {
                Text("Loading")
            }
        }
    }
}