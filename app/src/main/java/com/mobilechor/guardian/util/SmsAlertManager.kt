package com.mobilechor.guardian.util

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log

object SmsAlertManager {

    private const val TAG = "SmsAlertManager"

    fun sendEmergencySms(recipientPhone: String, messageText: String) {
        if (recipientPhone.isBlank() || messageText.isBlank()) return

        try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+
                com.mobilechor.guardian.MobileChorApp.instance.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val parts = smsManager.divideMessage(messageText)
            smsManager.sendMultipartTextMessage(recipientPhone, null, parts, null, null)
            Log.d(TAG, "Emergency SMS dispatched to $recipientPhone")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send emergency SMS: ${e.message}")
        }
    }
}
