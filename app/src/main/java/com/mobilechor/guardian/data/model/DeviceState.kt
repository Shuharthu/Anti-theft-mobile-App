package com.mobilechor.guardian.data.model

data class DeviceState(
    val deviceId: String = "",
    val model: String = "",
    val manufacturer: String = "",
    val androidVersion: String = "",
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val networkType: String = "WiFi",
    val ipAddress: String = "127.0.0.1",
    val wifiSsid: String = "Connected",
    val latitude: Double = 23.8103, // Default Dhaka coordinates for reference
    val longitude: Double = 90.4125,
    val locationAccuracy: Float = 10.0f,
    val lastLocationTime: Long = System.currentTimeMillis(),
    val isLostMode: Boolean = false,
    val isAlarmPlaying: Boolean = false,
    val isDeviceAdminActive: Boolean = false,
    val simOperator: String = "Active SIM",
    val simSerialHash: String = "",
    val isSimPresent: Boolean = true,
    val lastPingTimestamp: Long = System.currentTimeMillis(),
    val totalIntruderEvents: Int = 0
)
