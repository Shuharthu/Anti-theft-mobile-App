package com.mobilechor.guardian.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilechor.guardian.R
import com.mobilechor.guardian.data.model.EvidenceRecord
import com.mobilechor.guardian.ui.theme.*
import com.mobilechor.guardian.ui.viewmodel.GuardianViewModel
import com.mobilechor.guardian.util.SecurityUtils
import java.io.File

@Composable
fun EvidenceGalleryScreen(
    viewModel: GuardianViewModel
) {
    val context = LocalContext.current
    val evidenceList by viewModel.evidenceList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CleanBg)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.gallery_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
        Text(
            text = "Cryptographic tamper log & captured intruder photos",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMutedClean
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (evidenceList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(IconOrangeBg, CircleShape)
                            .border(1.dp, IconOrangeTint.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = IconOrangeTint, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.gallery_empty),
                        style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(evidenceList, key = { it.id }) { record ->
                    EvidenceCard(
                        record = record,
                        onDelete = { viewModel.deleteEvidence(record.id) },
                        onOpenMap = {
                            val uri = Uri.parse("https://maps.google.com/?q=${record.latitude},${record.longitude}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EvidenceCard(
    record: EvidenceRecord,
    onDelete: () -> Unit,
    onOpenMap: () -> Unit
) {
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
                            .size(10.dp)
                            .background(IconOrangeTint, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.triggerReason,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = IconOrangeTint)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMutedClean, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Photo Preview if file exists
            if (record.photoPath.isNotEmpty()) {
                val file = remember(record.photoPath) { File(record.photoPath) }
                if (file.exists()) {
                    val bitmap = remember(record.photoPath) { BitmapFactory.decodeFile(file.absolutePath) }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Intruder Evidence",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CleanBg),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            Text(
                text = "Captured: ${SecurityUtils.formatTimestamp(record.timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary
            )

            Text(
                text = "Location: ${record.latitude}, ${record.longitude} (Battery: ${record.batteryAtCapture}%)",
                style = MaterialTheme.typography.bodySmall,
                color = TextMutedClean
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    onClick = onOpenMap,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalBlue)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View on Map", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
