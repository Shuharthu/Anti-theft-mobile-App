package com.mobilechor.guardian.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.mobilechor.guardian.R
import com.mobilechor.guardian.ui.theme.*
import com.mobilechor.guardian.ui.viewmodel.GuardianViewModel
import com.mobilechor.guardian.util.PoliceReportGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoliceReportScreen(
    viewModel: GuardianViewModel
) {
    val context = LocalContext.current
    val deviceState by viewModel.deviceState.collectAsState()
    val ownerProfile by viewModel.ownerProfile.collectAsState()
    val evidenceList by viewModel.evidenceList.collectAsState()

    var selectedService by remember { mutableStateOf("Bangladesh Police GD Dossier") }
    var serviceExpanded by remember { mutableStateOf(false) }
    val serviceOptions = listOf("Bangladesh Police GD Dossier", "BTRC EIR Carrier Blacklist", "FRP Account Verification Report")

    var countryName by remember { mutableStateOf("Bangladesh") }
    var modelName by remember { mutableStateOf("${deviceState.manufacturer} ${deviceState.model}") }
    var imeiNumber by remember { mutableStateOf(deviceState.deviceId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CleanBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.report_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
        Text(
            text = stringResource(R.string.report_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = TextMutedClean
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Form Input Card (Matching Top Middle Phone in Reference UI)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CleanCardBorder))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "IMEI & Security Service Details",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = RoyalBlue)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Service Dropdown
                Text(text = "IMEI Service", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = serviceExpanded,
                    onExpandedChange = { serviceExpanded = !serviceExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedService,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = cleanTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = serviceExpanded,
                        onDismissRequest = { serviceExpanded = false }
                    ) {
                        serviceOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedService = option
                                    serviceExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Country Field
                Text(text = "Country Name", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = countryName,
                    onValueChange = { countryName = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = cleanTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mobile Model
                Text(text = "Mobile Model", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = cleanTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Enter IMEI Number
                Text(text = "Enter IMEI / Device ID", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = imeiNumber,
                    onValueChange = { imeiNumber = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = cleanTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        try {
                            val reportFile = PoliceReportGenerator.exportReportToFile(context)
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                reportFile
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/html"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "Mobile Chor - $selectedService: ${ownerProfile.ownerName}")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Submit & Share Report"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Report generated: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = CleanSurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = "Submit & Export Report", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Dossier Live Telemetry Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CleanCardBorder))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Certified Evidence Telemetry",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )

                Spacer(modifier = Modifier.height(12.dp))

                CleanTelemetryRow(label = "Complainant Owner", value = ownerProfile.ownerName.ifEmpty { "Registered Owner" })
                CleanTelemetryRow(label = "Primary Contact", value = ownerProfile.ownerPhone.ifEmpty { "Not specified" })
                CleanTelemetryRow(label = "Emergency Phone", value = ownerProfile.emergencyPhone.ifEmpty { "Not specified" })
                CleanTelemetryRow(label = "GPS Fix", value = "${"%.4f".format(deviceState.latitude)}, ${"%.4f".format(deviceState.longitude)}")
                CleanTelemetryRow(label = "Captured Evidence", value = "${evidenceList.size} Intruder Photos")
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // BTRC & Carrier Guide Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CleanCardBorder))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.report_btrc_guide_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = IconOrangeTint)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = stringResource(R.string.report_btrc_guide_step1), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.report_btrc_guide_step2), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.report_btrc_guide_step3), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun CleanTelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextMutedClean, fontSize = 13.sp)
        Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
