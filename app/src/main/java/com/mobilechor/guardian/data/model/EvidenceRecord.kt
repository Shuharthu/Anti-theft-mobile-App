package com.mobilechor.guardian.data.model

data class EvidenceRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val triggerReason: String = "Failed PIN Attempt",
    val photoPath: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val addressEstimate: String = "",
    val networkState: String = "",
    val batteryAtCapture: Int = 0,
    val isSyncedToCloud: Boolean = false
)
