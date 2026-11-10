package com.mobilechor.guardian.data.repository

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.google.gson.Gson
import com.mobilechor.guardian.data.model.DeviceState
import com.mobilechor.guardian.data.model.OwnerProfile
import com.mobilechor.guardian.receiver.GuardianDeviceAdminReceiver
import com.mobilechor.guardian.util.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GuardianRepository(private val context: Context) {

    private val gson = Gson()
    private val prefs: SharedPreferences = context.getSharedPreferences("mobile_chor_prefs", Context.MODE_PRIVATE)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    val evidenceRepository = EvidenceRepository(context)

    private val _ownerProfile = MutableStateFlow(loadOwnerProfile())
    val ownerProfile: StateFlow<OwnerProfile> = _ownerProfile.asStateFlow()

    private val _deviceState = MutableStateFlow(loadInitialDeviceState())
    val deviceState: StateFlow<DeviceState> = _deviceState.asStateFlow()

    private val _failedPinCount = MutableStateFlow(0)
    val failedPinCount: StateFlow<Int> = _failedPinCount.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // Backend URL for companion web dashboard (e.g., local server or cloud endpoint)
    var backendUrl: String
        get() = prefs.getString("backend_url", "http://10.0.2.2:3000") ?: "http://10.0.2.2:3000"
        set(value) = prefs.edit().putString("backend_url", value).apply()

    private fun loadOwnerProfile(): OwnerProfile {
        val json = prefs.getString("owner_profile", null)
        return if (!json.isNullOrEmpty()) {
            try {
                gson.fromJson(json, OwnerProfile::class.java)
            } catch (e: Exception) {
                OwnerProfile()
            }
        } else {
            OwnerProfile()
        }
    }

    private fun loadInitialDeviceState(): DeviceState {
        val (netType, ssid) = SecurityUtils.getNetworkInfo(context)
        return DeviceState(
            deviceId = SecurityUtils.getDeviceId(context),
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
            batteryPercent = SecurityUtils.getBatteryLevel(context),
            isCharging = SecurityUtils.isDeviceCharging(context),
            networkType = netType,
            wifiSsid = ssid,
            isDeviceAdminActive = isDeviceAdminActive()
        )
    }

    fun saveOwnerProfile(profile: OwnerProfile) {
        _ownerProfile.value = profile
        prefs.edit().putString("owner_profile", gson.toJson(profile)).apply()
        syncStateWithCloud()
    }

    fun updatePin(pin: String) {
        val cleanPin = pin.trim()
        val hashed = SecurityUtils.hashPin(cleanPin)
        val updated = _ownerProfile.value.copy(pinHash = hashed)
        saveOwnerProfile(updated)
    }

    fun verifyPin(pin: String): Boolean {
        val cleanPin = pin.trim()
        val storedHash = _ownerProfile.value.pinHash
        if (storedHash.isEmpty()) {
            // Self-heal: set pinHash to this PIN if storedHash was empty from previous initial state
            updatePin(cleanPin)
            _failedPinCount.value = 0
            return true
        }
        val isValid = SecurityUtils.verifyPin(cleanPin, storedHash)
        if (isValid) {
            _failedPinCount.value = 0
        } else {
            _failedPinCount.value += 1
        }
        return isValid
    }

    fun resetFailedPinCount() {
        _failedPinCount.value = 0
    }

    fun updateLocation(lat: Double, lng: Double, accuracy: Float) {
        val current = _deviceState.value
        val updated = current.copy(
            latitude = lat,
            longitude = lng,
            locationAccuracy = accuracy,
            lastLocationTime = System.currentTimeMillis(),
            lastPingTimestamp = System.currentTimeMillis()
        )
        _deviceState.value = updated
        syncStateWithCloud()
    }

    fun updateBattery(level: Int, isCharging: Boolean) {
        val current = _deviceState.value
        val updated = current.copy(batteryPercent = level, isCharging = isCharging)
        _deviceState.value = updated
        syncStateWithCloud()
    }

    fun setLostMode(enabled: Boolean) {
        val current = _deviceState.value
        val updated = current.copy(isLostMode = enabled)
        _deviceState.value = updated
        syncStateWithCloud()
    }

    fun setAlarmPlaying(isPlaying: Boolean) {
        val current = _deviceState.value
        val updated = current.copy(isAlarmPlaying = isPlaying)
        _deviceState.value = updated
        syncStateWithCloud()
    }

    fun isDeviceAdminActive(): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val component = ComponentName(context, GuardianDeviceAdminReceiver::class.java)
        return dpm?.isAdminActive(component) == true
    }

    fun updateAdminStatus() {
        val current = _deviceState.value
        _deviceState.value = current.copy(isDeviceAdminActive = isDeviceAdminActive())
    }

    fun refreshRuntimeTelemetry() {
        val (netType, ssid) = SecurityUtils.getNetworkInfo(context)
        val battery = SecurityUtils.getBatteryLevel(context)
        val charging = SecurityUtils.isDeviceCharging(context)
        val current = _deviceState.value
        _deviceState.value = current.copy(
            batteryPercent = battery,
            isCharging = charging,
            networkType = netType,
            wifiSsid = ssid,
            isDeviceAdminActive = isDeviceAdminActive(),
            lastPingTimestamp = System.currentTimeMillis()
        )
    }

    fun syncStateWithCloud() {
        repositoryScope.launch {
            try {
                refreshRuntimeTelemetry()
                val state = _deviceState.value
                val profile = _ownerProfile.value
                val payload = mapOf(
                    "deviceId" to state.deviceId,
                    "model" to state.model,
                    "manufacturer" to state.manufacturer,
                    "androidVersion" to state.androidVersion,
                    "batteryPercent" to state.batteryPercent,
                    "isCharging" to state.isCharging,
                    "networkType" to state.networkType,
                    "wifiSsid" to state.wifiSsid,
                    "latitude" to state.latitude,
                    "longitude" to state.longitude,
                    "accuracy" to state.locationAccuracy,
                    "isLostMode" to state.isLostMode,
                    "isAlarmPlaying" to state.isAlarmPlaying,
                    "isDeviceAdminActive" to state.isDeviceAdminActive,
                    "simOperator" to state.simOperator,
                    "lastSeen" to System.currentTimeMillis(),
                    "ownerName" to profile.ownerName,
                    "ownerEmail" to profile.ownerEmail,
                    "ownerPhone" to profile.ownerPhone,
                    "emergencyContactName" to profile.emergencyContactName,
                    "emergencyPhone" to profile.emergencyPhone,
                    "customLostMessage" to profile.customLostMessage,
                    "pinHash" to profile.pinHash,
                    "geofenceEnabled" to profile.isGeofenceEnabled,
                    "geofenceLat" to profile.geofenceLat,
                    "geofenceLng" to profile.geofenceLng,
                    "geofenceRadius" to profile.geofenceRadiusMeters
                )
                val body = gson.toJson(payload).toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$backendUrl/api/device/telemetry")
                    .post(body)
                    .build()
                httpClient.newCall(request).execute().close()
            } catch (ignored: Exception) {
                // Silently queue or fallback when offline
            }
        }
    }

    fun uploadEvidenceToCloud(record: com.mobilechor.guardian.data.model.EvidenceRecord) {
        repositoryScope.launch {
            try {
                var photoDataUrl = "/assets/sample_intruder.svg"
                if (record.photoPath.isNotEmpty()) {
                    val file = java.io.File(record.photoPath)
                    if (file.exists() && file.length() > 0) {
                        val bytes = file.readBytes()
                        val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        photoDataUrl = "data:image/jpeg;base64,$b64"
                    }
                }
                val payload = mapOf(
                    "id" to record.id,
                    "timestamp" to record.timestamp,
                    "triggerReason" to record.triggerReason,
                    "photoUrl" to photoDataUrl,
                    "latitude" to record.latitude,
                    "longitude" to record.longitude,
                    "addressEstimate" to record.addressEstimate,
                    "batteryAtCapture" to record.batteryAtCapture,
                    "networkState" to record.networkState
                )
                val body = gson.toJson(payload).toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$backendUrl/api/evidence")
                    .post(body)
                    .build()
                httpClient.newCall(request).execute().close()
            } catch (ignored: Exception) {
            }
        }
    }

    fun acknowledgeCommand(commandId: String, status: String = "EXECUTED", note: String = "") {
        if (commandId.isBlank()) return
        repositoryScope.launch {
            try {
                val payload = mapOf(
                    "id" to commandId,
                    "status" to status,
                    "note" to note,
                    "executedAt" to System.currentTimeMillis()
                )
                val body = gson.toJson(payload).toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$backendUrl/api/device/commands/ack")
                    .post(body)
                    .build()
                httpClient.newCall(request).execute().close()
            } catch (ignored: Exception) {
            }
        }
    }
}
