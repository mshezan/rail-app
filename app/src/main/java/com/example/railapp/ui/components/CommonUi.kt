package com.example.railapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.railapp.data.ThreatLevel
import com.example.railapp.ui.theme.*

@Composable
fun ThreatStatusBadge(
    threatLevel: ThreatLevel,
    modifier: Modifier = Modifier
) {
    val dark = isSystemInDarkTheme()
    val (bgColor, textColor, label) = when (threatLevel) {
        ThreatLevel.HIGH -> Triple(
            if (dark) StatusHighThreatDarkContainer else StatusHighThreatContainer,
            StatusHighThreat,
            "CRITICAL THREAT"
        )
        ThreatLevel.MEDIUM -> Triple(
            if (dark) StatusMediumThreatDarkContainer else StatusMediumThreatContainer,
            StatusMediumThreat,
            "WARNING THREAT"
        )
        ThreatLevel.LOW -> Triple(
            if (dark) StatusLowThreatDarkContainer else StatusLowThreatContainer,
            StatusLowThreat,
            "LOW THREAT"
        )
        ThreatLevel.NONE -> Triple(
            if (dark) StatusSafeDarkContainer else StatusSafeContainer,
            StatusSafe,
            "CLEAR / SAFE"
        )
    }

    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SyncBadge(
    isSynced: Boolean,
    modifier: Modifier = Modifier
) {
    val dark = isSystemInDarkTheme()
    val bgColor = if (isSynced) {
        if (dark) StatusSafeDarkContainer else StatusSafeContainer
    } else {
        if (dark) StatusInfoCloudDarkContainer else StatusInfoCloudContainer
    }
    val contentColor = if (isSynced) StatusSafe else StatusInfoCloud

    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = if (isSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isSynced) "SYNCED" else "LOCAL",
                color = contentColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    description: String,
    icon: ImageVector = Icons.Default.Info,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(vertical = 8.dp)
    )
}
