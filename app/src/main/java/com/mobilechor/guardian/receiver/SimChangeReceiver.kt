package com.mobilechor.guardian.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.mobilechor.guardian.MobileChorApp
import com.mobilechor.guardian.service.CameraCaptureService
import com.mobilechor.guardian.util.SmsAlertManager

class SimChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action.contains("SIM_STATE_CHANGED", ignoreCase = true) || action.contains("SIM_CARD_STATE_CHANGED", ignoreCase = true)) {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val simState = telephonyManager?.simState ?: TelephonyManager.SIM_STATE_UNKNOWN

            if (simState == TelephonyManager.SIM_STATE_READY || simState == TelephonyManager.SIM_STATE_ABSENT) {
                val operatorName = telephonyManager?.simOperatorName ?: "Unknown / Removed"
                val state = MobileChorApp.instance.repository.deviceState.value
                val profile = MobileChorApp.instance.repository.ownerProfile.value

                // If SIM is changed or removed, immediately capture photos
                val captureIntent = Intent(context, CameraCaptureService::class.java).apply {
                    this.action = CameraCaptureService.ACTION_BURST_CAPTURE
                    putExtra("trigger", "SIM Card Change/Removal Detected ($operatorName)")
                }
                try {
                    context.startForegroundService(captureIntent)
                } catch (ignored: Exception) {}

                // Send Silent Emergency SMS if backup phone is configured
                if (profile.emergencyPhone.isNotEmpty()) {
                    val alertMessage = "CRITICAL ALERT [Mobile Chor]: SIM card changed on ${profile.ownerName}'s phone! New Operator: $operatorName. Last GPS: https://maps.google.com/?q=${state.latitude},${state.longitude}"
                    SmsAlertManager.sendEmergencySms(profile.emergencyPhone, alertMessage)
                }
            }
        }
    }
}
