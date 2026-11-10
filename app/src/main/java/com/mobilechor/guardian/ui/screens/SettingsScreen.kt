package com.mobilechor.guardian.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilechor.guardian.MobileChorApp
import com.mobilechor.guardian.R
import com.mobilechor.guardian.receiver.GuardianDeviceAdminReceiver
import com.mobilechor.guardian.ui.theme.*
import com.mobilechor.guardian.ui.viewmodel.GuardianViewModel

@Composable
fun SettingsScreen(
    viewModel: GuardianViewModel
) {
    val context = LocalContext.current
    val ownerProfile by viewModel.ownerProfile.collectAsState()
    val deviceState by viewModel.deviceState.collectAsState()

    var showAudioDisclaimerDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showUninstallDialog by remember { mutableStateOf(false) }
    var backendUrlInput by remember { mutableStateOf(MobileChorApp.instance.repository.backendUrl) }

    // Change PIN dialog states
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmNewPinInput by remember { mutableStateOf("") }
    var pinDialogError by remember { mutableStateOf("") }

    // Uninstall / Deactivate Admin PIN state
    var uninstallPinInput by remember { mutableStateOf("") }
    var uninstallDialogError by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CleanBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
        Text(
            text = "Control security thresholds, PIN, uninstall protection & language",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMutedClean
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Change Security PIN Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CleanCardBorder))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showChangePinDialog = true }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(RoyalBlueLight, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.settings_change_pin),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Text(
                            text = "Update your 4-6 digit owner security PIN",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedClean
                        )
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMutedClean)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Uninstall Protection & Device Admin Management Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CleanCardBorder))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    if (deviceState.isDeviceAdminActive) IconOrangeBg else IconGreenBg,
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = if (deviceState.isDeviceAdminActive) IconOrangeTint else IconGreenTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.settings_uninstall_protection),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            Text(
                                text = if (deviceState.isDeviceAdminActive) "Active (Uninstallation Blocked)" else "Inactive",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (deviceState.isDeviceAdminActive) IconOrangeTint else IconGreenTint,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Android blocks uninstalling apps with active Device Admin. Enter your PIN below to deactivate Device Admin and uninstall the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedClean
                )
                Spacer(modifier = Modifier.height(14.dp))

                if (deviceState.isDeviceAdminActive) {
                    Button(
                        onClick = { showUninstallDialog = true },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberRed, contentColor = CleanSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Deactivate Admin & Uninstall App", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                            val adminComponent = ComponentName(context, GuardianDeviceAdminReceiver::class.java)
                            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, context.getString(R.string.device_admin_description))
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = CleanSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enable Device Admin Protection", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CleanCardBorder))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = RoyalBlue)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { viewModel.setLanguage(context, "en") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (ownerProfile.language == "en") RoyalBlue else CleanBg,
                            contentColor = if (ownerProfile.language == "en") CleanSurface else TextPrimary
                        ),
                        border = if (ownerProfile.language == "en") null else androidx.compose.foundation.BorderStroke(1.dp, CleanCardBorder)
                    ) {
                        Text(
                            text = "English",
                            fontWeight = FontWeight.Bold,
                            color = if (ownerProfile.language == "en") CleanSurface else TextPrimary
                        )
                    }

                    Button(
                        onClick = { viewModel.setLanguage(context, "bn") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (ownerProfile.language == "bn") IconOrangeTint else CleanBg,
                            contentColor = if (ownerProfile.language == "bn") CleanSurface else TextPrimary
                        ),
                        border = if (ownerProfile.language == "bn") null else androidx.compose.foundation.BorderStroke(1.dp, CleanCardBorder)
                    ) {
                        Text(
                            text = "বাংলা (Bangla)",
                            fontWeight = FontWeight.Bold,
                            color = if (ownerProfile.language == "bn") CleanSurface else TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tracking Frequency Interval
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CleanCardBorder))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_tracking_interval),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = RoyalBlue)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val intervals = listOf(1 to "1m (Realtime)", 15 to "15m (Balanced)", 60 to "1h (Eco)")
                    for ((mins, label) in intervals) {
                        val isSelected = ownerProfile.trackingIntervalMinutes == mins
                        Button(
                            onClick = { viewModel.updateTrackingInterval(mins) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) RoyalBlue else CleanBg,
                                contentColor = if (isSelected) CleanSurface else TextPrimary
                            ),
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, CleanCardBorder)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) CleanSurface else TextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Safe Zone Geofence, Biometric & Audio recording switches
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CleanCardBorder))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_geofence_toggle),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Text(
                            text = stringResource(R.string.settings_geofence_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedClean
                        )
                    }
                    Switch(
                        checked = ownerProfile.isGeofenceEnabled,
                        onCheckedChange = { viewModel.toggleGeofencing(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = RoyalBlue, checkedTrackColor = RoyalBlueLight)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = CleanCardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_biometric_toggle),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Text(
                            text = stringResource(R.string.settings_biometric_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedClean
                        )
                    }
                    Switch(
                        checked = ownerProfile.isBiometricEnabled,
                        onCheckedChange = { viewModel.toggleBiometrics(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = RoyalBlue, checkedTrackColor = RoyalBlueLight)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = CleanCardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_audio_toggle),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Text(
                            text = "Legal disclaimer applies. Disabled by default.",
                            style = MaterialTheme.typography.bodySmall,
                            color = IconOrangeTint
                        )
                    }
                    Switch(
                        checked = ownerProfile.isAudioRecordingOptIn,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showAudioDisclaimerDialog = true
                            } else {
                                viewModel.toggleAudioRecording(false)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = IconOrangeTint, checkedTrackColor = IconOrangeBg)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Companion Web Dashboard URL Config
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CleanCardBorder))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Companion Web Dashboard Endpoint",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = RoyalBlue)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = backendUrlInput,
                    onValueChange = { backendUrlInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = cleanTextFieldColors(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        MobileChorApp.instance.repository.backendUrl = backendUrlInput
                        viewModel.refreshTelemetry()
                        Toast.makeText(context, "Dashboard Endpoint Saved & Synced", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = CleanSurface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save & Sync", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    // Change Security PIN Dialog Modal
    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangePinDialog = false
                pinDialogError = ""
                currentPinInput = ""
                newPinInput = ""
                confirmNewPinInput = ""
            },
            containerColor = CleanSurface,
            titleContentColor = RoyalBlue,
            textContentColor = TextPrimary,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = RoyalBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Security PIN", fontWeight = FontWeight.Bold, color = RoyalBlue)
                }
            },
            text = {
                Column {
                    Text("Verify current PIN and enter your new 4-6 digit PIN", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = currentPinInput,
                        onValueChange = { if (it.length <= 6) currentPinInput = it },
                        label = { Text("Current Security PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = cleanTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 6) newPinInput = it },
                        label = { Text("New Security PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = cleanTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = confirmNewPinInput,
                        onValueChange = { if (it.length <= 6) confirmNewPinInput = it },
                        label = { Text("Confirm New Security PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = cleanTextFieldColors()
                    )

                    if (pinDialogError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(pinDialogError, color = CyberRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val repo = MobileChorApp.instance.repository
                        if (!repo.verifyPin(currentPinInput)) {
                            pinDialogError = "Current PIN is incorrect!"
                        } else if (newPinInput.length < 4) {
                            pinDialogError = "New PIN must be at least 4 digits!"
                        } else if (newPinInput != confirmNewPinInput) {
                            pinDialogError = "New PINs do not match!"
                        } else {
                            repo.updatePin(newPinInput)
                            Toast.makeText(context, "Security PIN updated successfully!", Toast.LENGTH_SHORT).show()
                            showChangePinDialog = false
                            currentPinInput = ""
                            newPinInput = ""
                            confirmNewPinInput = ""
                            pinDialogError = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = CleanSurface)
                ) {
                    Text("Update PIN", fontWeight = FontWeight.Bold, color = CleanSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Uninstall Protection / Deactivate Admin Modal
    if (showUninstallDialog) {
        AlertDialog(
            onDismissRequest = {
                showUninstallDialog = false
                uninstallPinInput = ""
                uninstallDialogError = ""
            },
            containerColor = CleanSurface,
            titleContentColor = CyberRed,
            textContentColor = TextPrimary,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CyberRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Deactivate Admin & Uninstall", fontWeight = FontWeight.Bold, color = CyberRed)
                }
            },
            text = {
                Column {
                    Text("Enter your security PIN to deactivate Device Admin and open the app uninstallation screen.", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = uninstallPinInput,
                        onValueChange = { if (it.length <= 6) uninstallPinInput = it },
                        label = { Text("Enter Security PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = cleanTextFieldColors()
                    )

                    if (uninstallDialogError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(uninstallDialogError, color = CyberRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val repo = MobileChorApp.instance.repository
                        if (repo.verifyPin(uninstallPinInput)) {
                            // Deactivate device admin
                            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                            val adminComponent = ComponentName(context, GuardianDeviceAdminReceiver::class.java)
                            dpm.removeActiveAdmin(adminComponent)
                            viewModel.refreshTelemetry()

                            showUninstallDialog = false
                            uninstallPinInput = ""
                            uninstallDialogError = ""

                            Toast.makeText(context, "Device Admin Deactivated. Requesting Uninstall...", Toast.LENGTH_SHORT).show()

                            // Launch OS uninstall dialog
                            val packageURI = Uri.parse("package:${context.packageName}")
                            val uninstallIntent = Intent(Intent.ACTION_DELETE, packageURI)
                            context.startActivity(uninstallIntent)
                        } else {
                            uninstallDialogError = "Incorrect Security PIN!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberRed, contentColor = CleanSurface)
                ) {
                    Text("Deactivate & Uninstall", fontWeight = FontWeight.Bold, color = CleanSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUninstallDialog = false }) {
                    Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Audio Opt-In Legal Disclaimer Modal
    if (showAudioDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showAudioDisclaimerDialog = false },
            containerColor = CleanSurface,
            titleContentColor = IconOrangeTint,
            textContentColor = TextPrimary,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = IconOrangeTint)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Legal Recording Notice", fontWeight = FontWeight.Bold, color = IconOrangeTint)
                }
            },
            text = {
                Text(
                    text = stringResource(R.string.perm_audio_legal_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleAudioRecording(true)
                        showAudioDisclaimerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IconOrangeTint, contentColor = CleanSurface)
                ) {
                    Text("I Acknowledge & Enable", fontWeight = FontWeight.Bold, color = CleanSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAudioDisclaimerDialog = false }) {
                    Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}
