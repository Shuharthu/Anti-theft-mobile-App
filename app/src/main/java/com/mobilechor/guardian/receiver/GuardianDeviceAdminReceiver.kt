package com.mobilechor.guardian.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.mobilechor.guardian.MobileChorApp
import com.mobilechor.guardian.service.CameraCaptureService

class GuardianDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        MobileChorApp.instance.repository.updateAdminStatus()
        Toast.makeText(context, "Mobile Chor Device Admin Activated", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        // Trigger 5-burst photo capture when someone attempts to remove admin
        val captureIntent = Intent(context, CameraCaptureService::class.java).apply {
            action = CameraCaptureService.ACTION_BURST_CAPTURE
            putExtra("trigger", "Device Admin Deactivation Attempt")
        }
        context.startForegroundService(captureIntent)

        return "WARNING: Mobile Chor Anti-Theft Protection will be deactivated. Photos of this attempt have been captured and transmitted to the registered owner!"
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        MobileChorApp.instance.repository.updateAdminStatus()
    }

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        // OS level lock screen failed password
        val captureIntent = Intent(context, CameraCaptureService::class.java).apply {
            action = CameraCaptureService.ACTION_CAPTURE_PHOTO
            putExtra("trigger", "Lock Screen Password Failed")
        }
        context.startForegroundService(captureIntent)
    }
}
