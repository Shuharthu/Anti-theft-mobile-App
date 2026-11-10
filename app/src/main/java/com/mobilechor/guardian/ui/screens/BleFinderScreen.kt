package com.mobilechor.guardian.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilechor.guardian.R
import com.mobilechor.guardian.ui.theme.*
import com.mobilechor.guardian.ui.viewmodel.GuardianViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BleFinderScreen(
    viewModel: GuardianViewModel
) {
    var isBroadcasting by remember { mutableStateOf(true) }
    var estimatedDistance by remember { mutableStateOf("Near (~2-4 meters)") }

    // Pulsing radar animation
    val infiniteTransition = rememberInfiniteTransition(label = "RadarTransition")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = 130f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseRadius"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CleanBg)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.radar_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
        Text(
            text = stringResource(R.string.radar_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = TextMutedClean,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Radar Canvas Display
        Box(
            modifier = Modifier
                .size(250.dp)
                .background(CleanSurface, CircleShape)
                .border(2.dp, CleanCardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)

                // Concentric circles
                drawCircle(color = CleanCardBorder, radius = 40.dp.toPx(), center = center, style = Stroke(width = 1.dp.toPx()))
                drawCircle(color = CleanCardBorder, radius = 80.dp.toPx(), center = center, style = Stroke(width = 1.dp.toPx()))
                drawCircle(color = CleanCardBorder, radius = 120.dp.toPx(), center = center, style = Stroke(width = 1.dp.toPx()))

                // Crosshairs
                drawLine(color = CleanCardBorder, start = Offset(center.x, 0f), end = Offset(center.x, size.height))
                drawLine(color = CleanCardBorder, start = Offset(0f, center.y), end = Offset(size.width, center.y))

                // Pulsing wave
                if (isBroadcasting) {
                    drawCircle(
                        color = RoyalBlue.copy(alpha = (1f - (pulseRadius / 130f)).coerceIn(0f, 0.6f)),
                        radius = pulseRadius * density,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Rotating Radar Sweep Line
                    val rad = Math.toRadians(sweepAngle.toDouble())
                    val endX = center.x + 120.dp.toPx() * cos(rad).toFloat()
                    val endY = center.y + 120.dp.toPx() * sin(rad).toFloat()
                    drawLine(
                        color = RoyalBlue,
                        start = center,
                        end = Offset(endX, endY),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Target Blip
                    val blipX = center.x + 60.dp.toPx() * cos(Math.toRadians(45.0)).toFloat()
                    val blipY = center.y + 60.dp.toPx() * sin(Math.toRadians(45.0)).toFloat()
                    drawCircle(color = IconOrangeTint, radius = 6.dp.toPx(), center = Offset(blipX, blipY))
                }
            }

            // Center beacon icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(RoyalBlueLight, CircleShape)
                    .border(2.dp, RoyalBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = RoyalBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Distance Indicator Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CleanCardBorder))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Signal Proximity Estimate", color = TextMutedClean, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = estimatedDistance,
                    color = IconOrangeTint,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { 0.75f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = RoyalBlue,
                    trackColor = CleanCardBorder,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val deviceState by viewModel.deviceState.collectAsState()

        // Chirp / Sound Button
        Button(
            onClick = {
                if (deviceState.isAlarmPlaying) {
                    viewModel.stopAlarm()
                } else {
                    viewModel.triggerAlarm()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (deviceState.isAlarmPlaying) CyberRed else RoyalBlue,
                contentColor = CleanSurface
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = if (deviceState.isAlarmPlaying) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (deviceState.isAlarmPlaying) "Stop Loud Tone" else "Chirp Loud Tone (Locate Phone)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Note: Bluetooth Radar is a near-field finder (~10-30m) designed for indoor recovery. City-wide offline tracking relies on Google's official Find My Device network.",
            color = TextMutedClean,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}
