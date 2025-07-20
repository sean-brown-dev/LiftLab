package com.browntowndev.liftlab.ui.views.main.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.ui.composables.LiftLabDialog
import com.browntowndev.liftlab.ui.composables.SignIntoFirebaseWithGoogleButton
import dev.gitlive.firebase.auth.FirebaseUser

@Composable
fun FirestoreSyncDialog(
    loginModalVisible: Boolean,
    firebaseUsername: String?,
    loggedIn: Boolean,
    firebaseError: String?,
    onDismiss: () -> Unit,
    onCreateAccount: (String, String) -> Unit,
    onLogin: (String, String) -> Unit,
    onLogout: () -> Unit,
    onSignedInWithGoogle: (Result<FirebaseUser?>) -> Unit,
    onSyncAll: () -> Unit,
) {
    LiftLabDialog(
        modifier = Modifier.padding(start = 15.dp, end = 15.dp, top = 15.dp),
        isVisible = loginModalVisible,
        header = "Cloud Upsert",
        onDismiss = onDismiss,
    ) {
        if (loggedIn) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.cloud_done),
                        contentDescription = "Logged In",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    firebaseUsername?.let { Text(text = it) }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onSyncAll,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            modifier = Modifier.padding(end = 12.dp),
                            painter = painterResource(id = R.drawable.cloud_sync),
                            contentDescription = "Upsert All",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            text = "Sync",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onLogout,
                ) {
                    Text(
                        text = "Logout",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 18.sp,
                    )
                }
            }
        } else {
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var isCreatingAccount by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SignIntoFirebaseWithGoogleButton(
                    onSignInComplete = onSignedInWithGoogle,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                if (firebaseError != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = firebaseError,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (isCreatingAccount) {
                            onCreateAccount(email, password)
                        } else {
                            onLogin(email, password)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isCreatingAccount) "Create Account" else "Login")
                }
                TextButton(
                    onClick = { isCreatingAccount = !isCreatingAccount },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        if (isCreatingAccount) "Already have an account? Login"
                        else "No account? Create one"
                    )
                }
            }
        }
    }
}