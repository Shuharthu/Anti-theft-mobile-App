package com.mobilechor.guardian.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilechor.guardian.R
import com.mobilechor.guardian.ui.theme.*
import com.mobilechor.guardian.ui.viewmodel.GuardianViewModel

@Composable
fun DashboardScreen(
    viewModel: GuardianViewModel,
    onNavigateToEvidence: () -> Unit
) {
    val context = LocalContext.current
    val deviceState by viewModel.deviceState.collectAsState()
    val ownerProfile by viewModel.ownerProfile.collectAsState()
    val evidenceList by viewModel.evidenceList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CleanBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcome Header & Profile Summary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Welcome to Unlocking & Security",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "Bound to Owner: ${ownerProfile.ownerName.ifEmpty { "Personal Device" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMutedClean
                )
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(RoyalBlueLight)
                    .border(1.5.dp, RoyalBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Device Status Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            shape = RoundedCornerShape(18.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CleanCardBorder))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                if (deviceState.isLostMode) CyberRed else CyberGreen,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (deviceState.isLostMode) stringResource(R.string.status_lost_mode) else stringResource(R.string.status_protected),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (deviceState.isLostMode) CyberRed else IconGreenTint
                            )
                        )
                        Text(
                            text = "Battery: ${deviceState.batteryPercent}% • Network: ${deviceState.networkType}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedClean
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (deviceState.isDeviceAdminActive) IconGreenBg else IconOrangeBg
                ) {
                    Text(
                        text = if (deviceState.isDeviceAdminActive) "Admin OK ✓" else "Admin Off",
                        color = if (deviceState.isDeviceAdminActive) IconGreenTint else IconOrangeTint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2x2 Clean Minimalist Service Cards Grid (Matching Visual Reference UI)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card 1: FRP & Admin Lock
            MinimalServiceCard(
                icon = Icons.Default.AdminPanelSettings,
                iconBg = IconGreenBg,
                iconTint = IconGreenTint,
                title = "FRP & Admin Lock",
                subtitle = if (deviceState.isDeviceAdminActive) "Protected" else "Tap to Enable",
                badgeText = if (deviceState.isDeviceAdminActive) "OK" else "!",
                modifier = Modifier.weight(1f)
            ) {
                viewModel.refreshTelemetry()
            }

            // Card 2: IMEI Info Checker
            MinimalServiceCard(
                icon = Icons.Default.Smartphone,
                iconBg = IconPurpleBg,
                iconTint = IconPurpleTint,
                title = "IMEI Info Checker",
                subtitle = "Hardware ID & Model",
                badgeText = ">",
                modifier = Modifier.weight(1f)
            ) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Device ID", deviceState.deviceId)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Device ID copied to clipboard: ${deviceState.deviceId}", Toast.LENGTH_LONG).show()
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card 3: Intruder Vault
            MinimalServiceCard(
                icon = Icons.Default.PhotoCamera,
                iconBg = IconOrangeBg,
                iconTint = IconOrangeTint,
                title = "Intruder Vault",
                subtitle = "Tamper Photo Logs",
                badgeText = "${evidenceList.size}",
                modifier = Modifier.weight(1f)
            ) {
                onNavigateToEvidence()
            }

            // Card 4: Proximity Radar
            MinimalServiceCard(
                icon = Icons.Default.Radar,
                iconBg = IconBlueBg,
                iconTint = IconBlueTint,
                title = "Bluetooth Radar",
                subtitle = "Indoor Finder",
                badgeText = ">",
                modifier = Modifier.weight(1f)
            ) {
                viewModel.refreshTelemetry()
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Top Secret & Emergency Secret Codes Section
        Text(
            text = "Top Emergency Actions & Secret Codes",
            style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold),
            modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 10.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CleanCardBorder))
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                SecretCodeRow(
                    icon = Icons.Default.Smartphone,
                    title = "Android Secret Code (*#06# IMEI Check)",
                    subtitle = "Device ID: ${deviceState.deviceId}",
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("IMEI/ID", deviceState.deviceId)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied Device ID: ${deviceState.deviceId}", Toast.LENGTH_SHORT).show()
                    }
                )

                HorizontalDivider(color = CleanCardBorder, modifier = Modifier.padding(horizontal = 12.dp))

                SecretCodeRow(
                    icon = if (deviceState.isAlarmPlaying) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    title = if (deviceState.isAlarmPlaying) "Stop Emergency Siren" else "Trigger Emergency Loud Siren",
                    subtitle = "Loud audio tone & vibration alert",
                    onClick = {
                        if (deviceState.isAlarmPlaying) viewModel.stopAlarm() else viewModel.triggerAlarm()
                    }
                )

                HorizontalDivider(color = CleanCardBorder, modifier = Modifier.padding(horizontal = 12.dp))

                SecretCodeRow(
                    icon = Icons.Default.Lock,
                    title = "Engage Instant Lost Mode Lock",
                    subtitle = "Overlay lockscreen & display emergency contact",
                    onClick = {
                        viewModel.toggleLostMode(true)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Customer Care Service Section (Matching Middle Bottom Phone)
        Text(
            text = "Customer Care Service & Support",
            style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold),
            modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 10.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CareActionButton(
                icon = Icons.Default.Phone,
                text = "Call Emergency Owner Contact (24/7)",
                containerColor = RoyalBlue
            ) {
                if (ownerProfile.emergencyPhone.isNotEmpty()) {
                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${ownerProfile.emergencyPhone}")
                    }
                    context.startActivity(dialIntent)
                } else {
                    Toast.makeText(context, "Please configure Emergency Contact in Settings", Toast.LENGTH_SHORT).show()
                }
            }

            CareActionButton(
                icon = Icons.Default.Sync,
                text = "Sync Telemetry with Cloud Dashboard",
                containerColor = RoyalBlue
            ) {
                viewModel.refreshTelemetry()
                Toast.makeText(context, "Live Telemetry Synced with Dashboard", Toast.LENGTH_SHORT).show()
            }

            CareActionButton(
                icon = Icons.Default.CameraAlt,
                text = "Test Silent Front Camera Capture",
                containerColor = RoyalBlue
            ) {
                viewModel.triggerTestPhotoCapture()
                Toast.makeText(context, "Silent Intruder Photo Captured!", Toast.LENGTH_SHORT).show()
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun MinimalServiceCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    badgeText: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(130.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CleanSurface),
        shape = RoundedCornerShape(20.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CleanCardBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(iconBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                }

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(CleanBg, CircleShape)
                        .border(1.dp, CleanCardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMutedClean
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedClean,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun SecretCodeRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(CleanBg, CircleShape)
                    .border(1.dp, CleanCardBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextMutedClean)
            }
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextMutedClean, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun CareActionButton(
    icon: ImageVector,
    text: String,
    containerColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = CleanSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(CleanSurface.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}
