package com.browntowndev.liftlab.ui.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmk.kmpauth.firebase.google.GoogleButtonUiContainerFirebase
import com.mmk.kmpauth.uihelper.google.GoogleSignInButton
import dev.gitlive.firebase.auth.FirebaseUser

@Composable
fun SignIntoFirebaseWithGoogleButton(
    onSignInComplete: (Result<FirebaseUser?>) -> Unit,
) {
    GoogleButtonUiContainerFirebase(onResult = onSignInComplete, linkAccount = false) {
        GoogleSignInButton(modifier = Modifier.fillMaxWidth().height(44.dp), fontSize = 19.sp) { this.onClick() }
    }
}
