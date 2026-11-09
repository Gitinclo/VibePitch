package com.gitinclo.vibepitch.ui.features.crm

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gitinclo.vibepitch.R
import com.gitinclo.vibepitch.ui.theme.EmeraldConversion
import com.gitinclo.vibepitch.ui.theme.AmberHighlight

@Composable
fun StorageSyncStatusDotChip(
    isDeviceOnline: Boolean,
    isGuestMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSavingLocally = !isDeviceOnline || isGuestMode
    val dotColor = when {
        !isDeviceOnline -> AmberHighlight
        isGuestMode -> Color(0xFF38BDF8)
        else -> EmeraldConversion
    }
    val statusLabel = when {
        !isDeviceOnline -> stringResource(R.string.status_offline_local)
        isGuestMode -> stringResource(R.string.status_local_storage)
        else -> stringResource(R.string.status_cloud_firestore)
    }
    val accessibilityDesc = when {
        !isDeviceOnline -> stringResource(R.string.offline_storage_banner_subtitle)
        isGuestMode -> stringResource(R.string.guest_local_storage_desc)
        else -> stringResource(R.string.cloud_storage_active_desc)
    }

    Surface(
        color = dotColor.copy(alpha = 0.14f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.45f)),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .semantics { contentDescription = accessibilityDesc }
            .testTag("storage_sync_status_chip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Outer halo + inner status dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = 0.28f))
                    .testTag("storage_sync_status_dot"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }

            Icon(
                imageVector = when {
                    !isDeviceOnline -> Icons.Default.CloudOff
                    isSavingLocally -> Icons.Default.Save
                    else -> Icons.Default.CloudDone
                },
                contentDescription = null,
                tint = dotColor,
                modifier = Modifier.size(14.dp)
            )

            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun OfflineStorageNoticeBanner(
    isDeviceOnline: Boolean,
    isGuestMode: Boolean,
    modifier: Modifier = Modifier
) {
    if (isDeviceOnline) return

    Surface(
        color = AmberHighlight.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, AmberHighlight.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("offline_local_storage_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(AmberHighlight.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AmberHighlight)
                        .testTag("offline_banner_status_dot")
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = stringResource(R.string.status_offline_local),
                tint = AmberHighlight,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.offline_storage_banner_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = AmberHighlight,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isGuestMode) {
                        stringResource(R.string.offline_storage_banner_subtitle)
                    } else {
                        stringResource(R.string.offline_storage_account_subtitle)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
