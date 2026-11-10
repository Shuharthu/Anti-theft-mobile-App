package com.mobilechor.guardian.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mobilechor.guardian.MobileChorApp
import com.mobilechor.guardian.service.CameraCaptureService
import com.mobilechor.guardian.util.SecurityUtils
import com.mobilechor.guardian.util.SmsAlertManager

class PowerAndBatteryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SHUTDOWN -> {
                // Power off / shutdown initiated
                val captureIntent = Intent(context, CameraCaptureService::class.java).apply {
                    action = CameraCaptureService.ACTION_BURST_CAPTURE
                    putExtra("trigger", "Device Power Off / Shutdown Attempt")
                }
                try {
                    context.startForegroundService(captureIntent)
                } catch (ignored: Exception) {}

                MobileChorApp.instance.repository.syncStateWithCloud()
            }

            Intent.ACTION_BATTERY_LOW -> {
                val batteryPct = SecurityUtils.getBatteryLevel(context)
                MobileChorApp.instance.repository.updateBattery(batteryPct, false)

                // Transmit final location ping before dying
                val profile = MobileChorApp.instance.repository.ownerProfile.value
                val state = MobileChorApp.instance.repository.deviceState.value

                if (profile.emergencyPhone.isNotEmpty()) {
                    val alert = "LOW BATTERY ALERT [Mobile Chor]: Battery at $batteryPct%. Final GPS Location: https://maps.google.com/?q=${state.latitude},${state.longitude}"
                    SmsAlertManager.sendEmergencySms(profile.emergencyPhone, alert)
                }
            }

            Intent.ACTION_BATTERY_OKAY -> {
                val batteryPct = SecurityUtils.getBatteryLevel(context)
                MobileChorApp.instance.repository.updateBattery(batteryPct, true)
            }
        }
    }
}
