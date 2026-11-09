package com.gitinclo.vibepitch.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gitinclo.vibepitch.auth.authStateFlow
import com.gitinclo.vibepitch.data.local.LocalGuestSessionManager
import com.gitinclo.vibepitch.data.remote.FirebaseEnvValidator
import com.gitinclo.vibepitch.ui.VibePitchApp
import com.gitinclo.vibepitch.ui.features.auth.AuthScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.flowOf

@Composable
fun VibePitchNavGraph(
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
                VibePitchApp(
                    currentUserId = uid,
                    isGuestMode = false,
                    guestSessionManager = guestSessionManager
                )
            }
        }

        guestStatus.isGuestActive -> {
            key("guest_${guestStatus.startedAtMillis}") {
                VibePitchApp(
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
