package com.browntowndev.liftlab.ui.views.main.lab

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LiftLibraryScreen

@Composable
fun ProgramManager(
    paddingValues: PaddingValues,
    programs: List<ProgramDto>,
    onCreateProgram: () -> Unit,
    onSetProgramAsActive: (programId: Long) -> Unit,
    onDeleteProgram: (programId: Long) -> Unit,
    onNavigateBack: () -> Unit,
) {
    LazyColumn (
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(color = MaterialTheme.colorScheme.primaryContainer),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        item {
            Row(
                modifier = Modifier.padding(start = 20.dp, top = 10.dp, end =15.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Name",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 14.sp,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(
                        text = "Active",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        items(programs) { program ->
            Card(
                modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                )
            ) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, top = 25.dp, bottom = 25.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = program.name,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                        RadioButton(
                            selected = program.isActive,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.outline,
                            ),
                            onClick = { onSetProgramAsActive(program.id) }
                        )
                        IconButton(onClick = { onDeleteProgram(program.id) }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                tint = MaterialTheme.colorScheme.error,
                                contentDescription = stringResource(R.string.delete_program),
                            )
                        }
                    }
                }
            }
        }

        item {
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = onCreateProgram) {
                    Text(
                        text = "Create New Program",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }

    BackHandler {
        onNavigateBack()
    }
}