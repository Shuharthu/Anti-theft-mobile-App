package com.mobilechor.guardian.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.BackHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilechor.guardian.MobileChorApp
import com.mobilechor.guardian.R
import com.mobilechor.guardian.service.CameraCaptureService
import com.mobilechor.guardian.ui.theme.*

import androidx.fragment.app.FragmentActivity
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import com.mobilechor.guardian.util.BiometricHelper

class LostModeActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lockscreen and turn screen on
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContent {
            MobileChorTheme {
                BackHandler(enabled = true) {
                    // Prevent back gesture from unlocking device
                }

                val repo = MobileChorApp.instance.repository
                val profile by repo.ownerProfile.collectAsState()
                val deviceState by repo.deviceState.collectAsState()
                var hasObservedLostMode by remember { mutableStateOf(deviceState.isLostMode) }
                var inputPin by remember { mutableStateOf("") }
                var errorText by remember { mutableStateOf("") }
                var attempts by remember { mutableIntStateOf(0) }

                LaunchedEffect(deviceState.isLostMode) {
                    if (deviceState.isLostMode) {
                        hasObservedLostMode = true
                    } else if (hasObservedLostMode) {
                        Toast.makeText(this@LostModeActivity, "Unlocked Remotely by Owner", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                val isBiometricAvailable = remember { BiometricHelper.isBiometricAvailable(this@LostModeActivity) }

                fun triggerBiometricVerification() {
                    BiometricHelper.promptBiometricUnlock(
                        activity = this@LostModeActivity,
                        title = "Face / Biometric Unlock",
                        subtitle = "Verify owner identity to deactivate lost mode",
                        onSuccess = {
                            repo.setLostMode(false)
                            repo.resetFailedPinCount()
                            Toast.makeText(this@LostModeActivity, "Face / Biometric Verified - Device Unlocked", Toast.LENGTH_SHORT).show()
                            finish()
                        },
                        onFailure = { err ->
                            attempts++
                            errorText = "Biometric Check Failed: $err"
                            if (attempts >= 2) {
                                errorText = getString(R.string.lock_intruder_captured)
                                val captureIntent = Intent(this@LostModeActivity, CameraCaptureService::class.java).apply {
                                    action = CameraCaptureService.ACTION_CAPTURE_PHOTO
                                    putExtra("trigger", "Failed Biometric / Face Recognition on Lost Screen")
                                }
                                startForegroundService(captureIntent)
                            }
                        }
                    )
                }

                // Automatically trigger Face Recognition prompt on screen load if enabled
                LaunchedEffect(Unit) {
                    if (profile.isBiometricEnabled && isBiometricAvailable) {
                        triggerBiometricVerification()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CyberNavyDark
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Header Warning
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 28.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .background(CyberRed.copy(alpha = 0.2f), CircleShape)
                                    .border(2.dp, CyberRed, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Lost Alert",
                                    tint = CyberRed,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = stringResource(R.string.status_lost_mode),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CyberRed
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = profile.customLostMessage.ifEmpty {
                                    stringResource(R.string.custom_lost_message_default)
                                },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = TextWhite,
                                    lineHeight = 22.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                        }

                        // Face / Biometric Scan Button if available
                        if (isBiometricAvailable) {
                            OutlinedButton(
                                onClick = { triggerBiometricVerification() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = NeonCyan.copy(alpha = 0.15f),
                                    contentColor = NeonCyan
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(NeonCyan)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = "Face Recognition",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.btn_unlock_face),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Big Emergency Dial Button
                        if (profile.emergencyPhone.isNotEmpty()) {
                            Button(
                                onClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${profile.emergencyPhone}")
                                    }
                                    startActivity(dialIntent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberGreen,
                                    contentColor = CyberNavyDark
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.lock_call_owner, profile.emergencyPhone),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Owner PIN Unlock Section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
                            shape = RoundedCornerShape(16.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberNavyBorder))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.lock_enter_pin),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NeonCyan
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = inputPin,
                                    onValueChange = { if (it.length <= 6) inputPin = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    visualTransformation = PasswordVisualTransformation(),
                                    placeholder = { Text("••••", color = TextDim) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = CyberNavyBorder,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite
                                    )
                                )
                                if (errorText.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = errorText,
                                        color = CyberRed,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        if (repo.verifyPin(inputPin)) {
                                            repo.setLostMode(false)
                                            Toast.makeText(this@LostModeActivity, "Device Unlocked", Toast.LENGTH_SHORT).show()
                                            finish()
                                        } else {
                                            attempts++
                                            inputPin = ""
                                            if (attempts >= 2) {
                                                errorText = getString(R.string.lock_intruder_captured)
                                                // Trigger Intruder photo capture
                                                val captureIntent = Intent(this@LostModeActivity, CameraCaptureService::class.java).apply {
                                                    action = CameraCaptureService.ACTION_CAPTURE_PHOTO
                                                    putExtra("trigger", "Failed PIN on Lost Mode Screen")
                                                }
                                                startForegroundService(captureIntent)
                                            } else {
                                                errorText = getString(R.string.lock_wrong_pin, attempts)
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberNavyDark),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Unlock with PIN", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        // Prevent back button from bypassing lost mode
    }
}
