package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.auth.onGoogleSignInClicked
import com.example.data.model.BusinessAccountProfile
import com.example.data.model.NicheKey
import com.example.ui.theme.EmeraldConversion
import com.example.ui.theme.ObsidianSlate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PermanentBusinessAccountDialog(
    initialProfile: BusinessAccountProfile?,
    isGuestMode: Boolean,
    onDismiss: () -> Unit,
    onStageProfileForGoogleUpgrade: (BusinessAccountProfile) -> Unit,
    onSaveProfileDirectly: (BusinessAccountProfile) -> Unit,
    onGoogleAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var agencyName by remember {
        mutableStateOf(
            initialProfile?.agencyName?.ifBlank { "VibeFlow Local Studio" }
                ?: "VibeFlow Local Studio"
        )
    }
    var ownerName by remember {
        mutableStateOf(initialProfile?.ownerName ?: "")
    }
    var businessEmail by remember {
        mutableStateOf(initialProfile?.businessEmail ?: "")
    }
    var businessPhone by remember {
        mutableStateOf(initialProfile?.businessPhone ?: "")
    }
    var primaryNiche by remember {
        mutableStateOf(
            initialProfile?.primaryNicheKey?.let { NicheKey.fromKey(it) } ?: NicheKey.HOME_TRADES
        )
    }
    var websiteUrl by remember {
        mutableStateOf(initialProfile?.websiteUrl ?: "")
    }
    var isAuthenticating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isAuthenticating) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = null,
                tint = EmeraldConversion,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = if (isGuestMode) {
                    "Upgrade to Permanent Business Account"
                } else {
                    "Permanent Business Account Profile"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isGuestMode) {
                        "Enter your agency or freelance business details below, then verify with Google Sign-In to migrate all guest prospects, leads, and 14-day progress to your permanent cloud account."
                    } else {
                        "Manage your verified agency or freelance business details associated with your permanent cloud workspace."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = agencyName,
                    onValueChange = { agencyName = it },
                    label = { Text("Business / Agency Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("business_profile_agency_input")
                )

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Owner Full Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("business_profile_owner_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = businessPhone,
                        onValueChange = { businessPhone = it },
                        label = { Text("Business Phone") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("business_profile_phone_input")
                    )

                    OutlinedTextField(
                        value = businessEmail,
                        onValueChange = { businessEmail = it },
                        label = { Text("Business Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("business_profile_email_input")
                    )
                }

                Text(
                    text = "Primary Local Business Niche",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NicheKey.entries.forEach { niche ->
                        FilterChip(
                            selected = primaryNiche == niche,
                            onClick = { primaryNiche = niche },
                            label = { Text(niche.shortBadge) }
                        )
                    }
                }

                OutlinedTextField(
                    value = websiteUrl,
                    onValueChange = { websiteUrl = it },
                    label = { Text("Website / Calendly / Portfolio Link") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("business_profile_website_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (agencyName.isBlank() || ownerName.isBlank()) {
                        errorMessage = "Please enter your Business/Agency Name and Owner Name."
                        return@Button
                    }
                    val draft = BusinessAccountProfile(
                        id = "profile",
                        agencyName = agencyName.trim(),
                        ownerName = ownerName.trim(),
                        businessEmail = businessEmail.trim(),
                        businessPhone = businessPhone.trim(),
                        primaryNicheKey = primaryNiche.key,
                        websiteUrl = websiteUrl.trim(),
                        upgradedFromGuest = isGuestMode
                    )
                    if (isGuestMode) {
                        onStageProfileForGoogleUpgrade(draft)
                        isAuthenticating = true
                        errorMessage = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isAuthenticating = false
                                onDismiss()
                                onGoogleAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isAuthenticating = false
                                errorMessage = msg
                            },
                            scope = coroutineScope,
                            onAuthCancelled = {
                                isAuthenticating = false
                            }
                        )
                    } else {
                        onSaveProfileDirectly(draft)
                    }
                },
                enabled = !isAuthenticating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldConversion,
                    contentColor = ObsidianSlate
                ),
                modifier = Modifier.testTag("confirm_business_account_upgrade_button")
            ) {
                if (isAuthenticating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = ObsidianSlate,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isGuestMode) {
                            "Sign In with Google & Migrate Data"
                        } else {
                            "Save Business Profile"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isAuthenticating
            ) {
                Text("Cancel")
            }
        }
    )
}
