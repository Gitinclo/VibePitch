package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.authStateFlow
import com.example.data.local.LocalGuestSessionManager
import com.example.data.remote.FirebaseEnvValidator
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.VibePitchWorkspaceScreen
import com.example.ui.theme.VibePitchTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.flowOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VibePitchTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(
    auth: FirebaseAuth? = FirebaseEnvValidator.getAuthOrNull(LocalContext.current)
) {
    val context = LocalContext.current
    val guestSessionManager = remember { LocalGuestSessionManager(context) }
    val guestStatus by guestSessionManager.guestStatus.collectAsStateWithLifecycle()
    val userFlow = remember(auth) {
        auth?.authStateFlow() ?: flowOf(null)
    }
    val currentUser by userFlow.collectAsStateWithLifecycle(initialValue = auth?.currentUser)
    val user = currentUser

    when {
        user != null -> {
            val uid = user.uid
            key("auth_$uid") {
                VibePitchWorkspaceScreen(
                    currentUserId = uid,
                    isGuestMode = false,
                    guestSessionManager = guestSessionManager
                )
            }
        }

        guestStatus.isGuestActive -> {
            key("guest_${guestStatus.startedAtMillis}") {
                VibePitchWorkspaceScreen(
                    currentUserId = LocalGuestSessionManager.GUEST_USER_ID,
                    isGuestMode = true,
                    guestSessionManager = guestSessionManager
                )
            }
        }

        else -> {
            AuthScreen(
                onAuthSuccess = {},
                onStartGuestAccount = {
                    guestSessionManager.startOrResumeGuestSession()
                }
            )
        }
    }
}
