package com.mobilechor.guardian

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.mobilechor.guardian.data.repository.GuardianRepository

class MobileChorApp : Application() {

    companion object {
        const val CHANNEL_SERVICE = "channel_guardian_service"
        const val CHANNEL_ALERTS = "channel_security_alerts"
        const val CHANNEL_INTRUDER = "channel_intruder_capture"

        lateinit var instance: MobileChorApp
            private set
    }

    lateinit var repository: GuardianRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = GuardianRepository(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Mobile Chor Protection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows that active anti-theft monitoring and owner sync is running"
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Security & Tamper Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts for SIM changes, power tamper, and low battery pings"
                enableVibration(true)
            }

            val intruderChannel = NotificationChannel(
                CHANNEL_INTRUDER,
                "Intruder Photo Captures",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when unauthorized PIN attempts trigger photo evidence"
            }

            notificationManager.createNotificationChannels(listOf(serviceChannel, alertsChannel, intruderChannel))
        }
    }
}
