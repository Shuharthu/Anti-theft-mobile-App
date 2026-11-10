package com.mobilechor.guardian.data.model

data class OwnerProfile(
    val ownerName: String = "",
    val ownerEmail: String = "",
    val ownerPhone: String = "",
    val emergencyContactName: String = "",
    val emergencyPhone: String = "",
    val emergencyEmail: String = "",
    val pinHash: String = "",
    val customLostMessage: String = "This phone is lost or stolen. Please contact the owner immediately. Police report filed.",
    val language: String = "en", // "en" or "bn"
    val trackingIntervalMinutes: Int = 15,
    val isGeofenceEnabled: Boolean = false,
    val geofenceLat: Double = 23.8103,
    val geofenceLng: Double = 90.4125,
    val geofenceRadiusMeters: Float = 200.0f,
    val isAudioRecordingOptIn: Boolean = false,
    val isBiometricEnabled: Boolean = true,
    val isOnboardingComplete: Boolean = false,
    val cloudSyncToken: String = ""
)
