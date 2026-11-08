package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.auth.onGoogleSignInClicked
import com.example.data.local.LocalGuestSessionManager
import com.example.data.model.BusinessAccountProfile
import com.example.data.model.GuestLifecycleStatus
import com.example.data.model.NicheKey
import com.example.ui.theme.AmberHighlight
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.EmeraldConversion
import com.example.ui.theme.ObsidianSlate

@Composable
fun GuestLifecycleBanner(
    guestStatus: GuestLifecycleStatus,
    onUpgradeClicked: () -> Unit
) {
    if (!guestStatus.isGuestActive) return
    val isDay20Window = guestStatus.currentDayNumber >= LocalGuestSessionManager.GUEST_UPGRADE_PROMPT_DAY
    val bannerColor = if (isDay20Window) AmberHighlight else ElectricIndigo

    Surface(
        color = bannerColor.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, bannerColor.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("guest_lifecycle_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isDay20Window) {
                        Icons.Default.NotificationsActive
                    } else {
                        Icons.Default.BusinessCenter
                    },
                    contentDescription = null,
                    tint = bannerColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isDay20Window) {
                            "Day ${guestStatus.currentDayNumber} of 30 • Upgrade to Permanent Business Account"
                        } else {
                            "30-Day Guest Account • Day ${guestStatus.currentDayNumber} of 30 (${guestStatus.daysRemaining}d left)"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = bannerColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isDay20Window) {
                            "Only ${guestStatus.daysRemaining} days left in guest mode. Activate your permanent business account to keep your CRM synced."
                        } else {
                            "Full access for 30 days • Reminder on Day 20 to register your permanent business account."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onUpgradeClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = bannerColor,
                    contentColor = if (isDay20Window) ObsidianSlate else Color.White
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier.testTag("guest_banner_upgrade_button")
            ) {
                Text(
                    text = "Upgrade",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun Day20MilestoneDialog(
    guestStatus: GuestLifecycleStatus,
    prospectCount: Int,
    leadCount: Int,
    onUpgradeNow: () -> Unit,
    onDismissForToday: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissForToday,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AmberHighlight.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = AmberHighlight,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                text = "Day ${guestStatus.currentDayNumber} Milestone: Activate Your Permanent Business Account",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "You've reached Day ${guestStatus.currentDayNumber} of your 30-day guest workspace (${guestStatus.daysRemaining} days remaining before guest access ends on Day 30).",
                    style = MaterialTheme.typography.bodyMedium
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Ready for Cloud Migration:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• $prospectCount Local Business Prospects & Brand Configs",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• $leadCount Captured Quote & Booking Leads",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Your 14-Day Outreach Checklist Progress",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Text(
                    text = "Upgrade now to register your permanent business profile and back up all records to the cloud.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onUpgradeNow,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldConversion,
                    contentColor = ObsidianSlate
                ),
                modifier = Modifier.testTag("day20_modal_upgrade_button")
            ) {
                Text("Activate Permanent Business Account", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissForToday,
                modifier = Modifier.testTag("day20_modal_dismiss_button")
            ) {
                Text("Remind Me Later")
            }
        }
    )
}

@Composable
fun Day30ExpiredLockScreen(
    guestStatus: GuestLifecycleStatus,
    prospectCount: Int,
    leadCount: Int,
    onUpgradeClicked: () -> Unit,
    onExitGuestClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianSlate, Color(0xFF152238), ObsidianSlate)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.5.dp, AmberHighlight.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(24.dp)
                .testTag("day30_expired_lock_card")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AmberHighlight.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockClock,
                        contentDescription = null,
                        tint = AmberHighlight,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Text(
                    text = "30-Day Guest Access Complete (Day ${guestStatus.currentDayNumber})",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Your 30-day guest period has ended. All of your prospects, custom demo brands, and captured leads are safely preserved on this device—activate your Permanent Business Account to unlock and sync them to the cloud.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1)
                )

                Surface(
                    color = ObsidianSlate.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$prospectCount",
                                style = MaterialTheme.typography.headlineMedium,
                                color = EmeraldConversion,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Saved Prospects",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$leadCount",
                                style = MaterialTheme.typography.headlineMedium,
                                color = AmberHighlight,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Captured Leads",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Button(
                    onClick = onUpgradeClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldConversion,
                        contentColor = ObsidianSlate
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("day30_activate_business_account_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Activate Permanent Business Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onExitGuestClicked,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Back to Sign-In Screen",
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }
    }
}

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
        mutableStateOf(initialProfile?.agencyName?.ifBlank { "VibeFlow Local Studio" } ?: "VibeFlow Local Studio")
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
