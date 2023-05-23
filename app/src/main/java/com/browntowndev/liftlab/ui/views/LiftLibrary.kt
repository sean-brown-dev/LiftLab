package com.browntowndev.liftlab.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.core.data.entities.Lift
import com.browntowndev.liftlab.ui.viewmodels.LiftLibraryViewModel
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.getViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext


@Composable
fun LiftLibrary(paddingValues: PaddingValues) {
    val liftLibraryViewModel: LiftLibraryViewModel = getViewModel()
    val allLifts = remember { mutableStateListOf<Lift>() }

    LaunchedEffect(Unit) {
            val liftEntities = withContext(Dispatchers.IO) {
                suspendCancellableCoroutine<List<Lift>> { continuation ->
                    launch {
                        try {
                            val result = liftLibraryViewModel.getAllLifts()
                            continuation.resume(result, null)
                        } catch (e: Exception) {
                            continuation.cancel(e)
                        }
                    }
                }
            }

            allLifts.addAll(liftEntities)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .wrapContentSize(Alignment.Center)
            .padding(paddingValues)
    ) {
        allLifts.forEach{ lift ->
            Text(
                text = lift.name,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center,
                fontSize = 20.sp
            )
        }
    }
}