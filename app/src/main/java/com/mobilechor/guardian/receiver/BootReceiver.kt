package com.mobilechor.guardian.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mobilechor.guardian.service.GuardianService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val serviceIntent = Intent(context, GuardianService::class.java).apply {
                action = GuardianService.ACTION_START_MONITORING
            }
            context.startForegroundService(serviceIntent)
        }
    }
}
