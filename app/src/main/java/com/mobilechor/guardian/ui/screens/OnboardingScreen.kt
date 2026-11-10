package com.mobilechor.guardian.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilechor.guardian.R
import com.mobilechor.guardian.data.model.OwnerProfile
import com.mobilechor.guardian.receiver.GuardianDeviceAdminReceiver
import com.mobilechor.guardian.ui.theme.*
import com.mobilechor.guardian.ui.viewmodel.GuardianViewModel

@Composable
fun OnboardingScreen(
    viewModel: GuardianViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(1) }
    val profile by viewModel.ownerProfile.collectAsState()

    var ownerName by remember { mutableStateOf(profile.ownerName) }
    var ownerEmail by remember { mutableStateOf(profile.ownerEmail) }
    var ownerPhone by remember { mutableStateOf(profile.ownerPhone) }
    var emergencyContactName by remember { mutableStateOf(profile.emergencyContactName) }
    var emergencyPhone by remember { mutableStateOf(profile.emergencyPhone) }

    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }

    val adminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshTelemetry()
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Camera permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            Toast.makeText(context, "Location permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    val phonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[android.Manifest.permission.READ_PHONE_STATE] == true) {
            Toast.makeText(context, "Phone state permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = CleanBg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(RoyalBlueLight, CircleShape)
                            .border(1.dp, RoyalBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = RoyalBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue
                        )
                    )
                }

                // Bilingual language toggle (EN / বাং)
                Row(
                    modifier = Modifier
                        .background(CleanSurface, RoundedCornerShape(20.dp))
                        .border(1.dp, CleanCardBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EN",
                        color = if (profile.language == "en") RoyalBlue else TextMutedClean,
                        fontWeight = if (profile.language == "en") FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable { viewModel.setLanguage(context, "en") }
                            .padding(horizontal = 4.dp)
                    )
                    Text(text = "|", color = CleanCardBorder, fontSize = 12.sp)
                    Text(
                        text = "বাং",
                        color = if (profile.language == "bn") IconOrangeTint else TextMutedClean,
                        fontWeight = if (profile.language == "bn") FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable { viewModel.setLanguage(context, "bn") }
                            .padding(horizontal = 4.dp)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step Progress Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                for (i in 1..4) {
                    Box(
                        modifier = Modifier
                            .size(if (i == currentStep) 24.dp else 12.dp)
                            .padding(horizontal = 3.dp)
                            .background(
                                color = if (i <= currentStep) RoyalBlue else CleanCardBorder,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (i == currentStep) {
                            Text(text = "$i", fontSize = 11.sp, color = CleanSurface, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (i < 4) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(2.dp)
                                .align(Alignment.CenterVertically)
                                .background(if (i < currentStep) RoyalBlue else CleanCardBorder)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (currentStep) {
                1 -> {
                    // Step 1: Legal Guardrail & Ethical Disclosures
                    Text(
                        text = stringResource(R.string.onboarding_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.onboarding_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMutedClean,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CleanSurface),
                        shape = RoundedCornerShape(20.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CleanCardBorder))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = RoyalBlue)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.disclosure_guardrail_title),
                                    style = MaterialTheme.typography.titleMedium.copy(color = RoyalBlue, fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = stringResource(R.string.disclosure_guardrail_1), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = stringResource(R.string.disclosure_guardrail_2), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = stringResource(R.string.disclosure_guardrail_3), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = stringResource(R.string.disclosure_guardrail_4), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { currentStep = 2 },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = CleanSurface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(text = stringResource(R.string.btn_agree_continue), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                2 -> {
                    // Step 2: Permissions Pre-Disclosure Cards
                    Text(
                        text = "Permissions & Pre-Disclosures",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Why Mobile Chor requires these specific OS capabilities",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMutedClean,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val hasAdmin = viewModel.deviceState.collectAsState().value.isDeviceAdminActive
                    val hasCamera = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.CAMERA
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    val hasLocation = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    val hasPhone = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.READ_PHONE_STATE
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    PermissionDisclosureCard(
                        icon = Icons.Default.AdminPanelSettings,
                        title = stringResource(R.string.perm_admin_title),
                        description = stringResource(R.string.perm_admin_desc),
                        actionText = if (hasAdmin) "Granted ✓" else stringResource(R.string.btn_grant_admin),
                        iconBg = IconOrangeBg,
                        tint = IconOrangeTint
                    ) {
                        if (!hasAdmin) {
                            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                            val adminComponent = ComponentName(context, GuardianDeviceAdminReceiver::class.java)
                            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, context.getString(R.string.device_admin_description))
                            }
                            adminLauncher.launch(intent)
                        } else {
                            Toast.makeText(context, "Device Admin is already granted", Toast.LENGTH_SHORT).show()
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionDisclosureCard(
                        icon = Icons.Default.CameraAlt,
                        title = stringResource(R.string.perm_camera_title),
                        description = stringResource(R.string.perm_camera_desc),
                        actionText = if (hasCamera) "Granted ✓" else stringResource(R.string.btn_grant_camera),
                        iconBg = IconBlueBg,
                        tint = RoyalBlue
                    ) {
                        if (!hasCamera) {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        } else {
                            Toast.makeText(context, "Camera permission already granted", Toast.LENGTH_SHORT).show()
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionDisclosureCard(
                        icon = Icons.Default.LocationOn,
                        title = stringResource(R.string.perm_location_title),
                        description = stringResource(R.string.perm_location_desc),
                        actionText = if (hasLocation) "Granted ✓" else stringResource(R.string.btn_grant_location),
                        iconBg = IconGreenBg,
                        tint = IconGreenTint
                    ) {
                        if (!hasLocation) {
                            val perms = mutableListOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                            locationPermissionLauncher.launch(perms.toTypedArray())
                        } else {
                            Toast.makeText(context, "Location permission already granted", Toast.LENGTH_SHORT).show()
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionDisclosureCard(
                        icon = Icons.Default.SimCard,
                        title = stringResource(R.string.perm_phone_title),
                        description = stringResource(R.string.perm_phone_desc),
                        actionText = if (hasPhone) "Granted ✓" else stringResource(R.string.btn_grant_phone),
                        iconBg = IconOrangeBg,
                        tint = IconOrangeTint
                    ) {
                        if (!hasPhone) {
                            phonePermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.READ_PHONE_STATE,
                                    android.Manifest.permission.SEND_SMS
                                )
                            )
                        } else {
                            Toast.makeText(context, "Phone permission already granted", Toast.LENGTH_SHORT).show()
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(
                            onClick = { currentStep = 1 },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = stringResource(R.string.btn_back), color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = { currentStep = 3 },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = CleanSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = stringResource(R.string.btn_next), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                3 -> {
                    // Step 3: Owner & Backup Emergency Binding
                    Text(
                        text = stringResource(R.string.setup_owner_account_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text(stringResource(R.string.owner_name_label), color = TextSecondary, fontWeight = FontWeight.Medium) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        colors = cleanTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = ownerEmail,
                        onValueChange = { ownerEmail = it },
                        label = { Text(stringResource(R.string.owner_email_label), color = TextSecondary, fontWeight = FontWeight.Medium) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        colors = cleanTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = ownerPhone,
                        onValueChange = { ownerPhone = it },
                        label = { Text(stringResource(R.string.owner_phone_label), color = TextSecondary, fontWeight = FontWeight.Medium) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        colors = cleanTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = CleanCardBorder)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Emergency Recovery Contact (Backup Phone)",
                        style = MaterialTheme.typography.titleMedium.copy(color = IconOrangeTint, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = emergencyContactName,
                        onValueChange = { emergencyContactName = it },
                        label = { Text(stringResource(R.string.backup_contact_name_label), color = TextSecondary, fontWeight = FontWeight.Medium) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        colors = cleanTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = emergencyPhone,
                        onValueChange = { emergencyPhone = it },
                        label = { Text(stringResource(R.string.backup_phone_label), color = TextSecondary, fontWeight = FontWeight.Medium) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        colors = cleanTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(
                            onClick = { currentStep = 2 },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = stringResource(R.string.btn_back), color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                if (ownerName.isBlank() || emergencyPhone.isBlank()) {
                                    Toast.makeText(context, "Please provide owner name & emergency phone", Toast.LENGTH_SHORT).show()
                                } else {
                                    currentStep = 4
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = CleanSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = stringResource(R.string.btn_next), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                4 -> {
                    // Step 4: PIN Setup & Finish
                    Text(
                        text = stringResource(R.string.setup_pin_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.setup_pin_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMutedClean,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6) pin = it },
                        label = { Text(stringResource(R.string.pin_label), color = TextSecondary, fontWeight = FontWeight.Medium) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        colors = cleanTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 6) confirmPin = it },
                        label = { Text(stringResource(R.string.pin_confirm_label), color = TextSecondary, fontWeight = FontWeight.Medium) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        colors = cleanTextFieldColors()
                    )

                    if (pinError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = pinError, color = CyberRed, style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = {
                            if (pin.length < 4) {
                                pinError = context.getString(R.string.pin_invalid_error)
                            } else if (pin != confirmPin) {
                                pinError = context.getString(R.string.pin_mismatch_error)
                            } else {
                                val updatedProfile = profile.copy(
                                    ownerName = ownerName,
                                    ownerEmail = ownerEmail,
                                    ownerPhone = ownerPhone,
                                    emergencyContactName = emergencyContactName,
                                    emergencyPhone = emergencyPhone
                                )
                                viewModel.completeOnboarding(updatedProfile, pin)
                                onComplete()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = CleanSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = stringResource(R.string.btn_finish), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun PermissionDisclosureCard(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String,
    iconBg: Color,
    tint: Color,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CleanSurface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CleanCardBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(iconBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = TextMutedClean)
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onAction,
                modifier = Modifier.align(Alignment.End),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = iconBg, contentColor = tint)
            ) {
                Text(text = actionText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tint)
            }
        }
    }
}
