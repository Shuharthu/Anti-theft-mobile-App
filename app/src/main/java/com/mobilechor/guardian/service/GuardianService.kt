package com.mobilechor.guardian.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mobilechor.guardian.MainActivity
import com.mobilechor.guardian.MobileChorApp
import com.mobilechor.guardian.R
import com.mobilechor.guardian.ui.screens.LostModeActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class GuardianService : Service() {

    companion object {
        const val ACTION_START_MONITORING = "ACTION_START_MONITORING"
        const val ACTION_STOP_MONITORING = "ACTION_STOP_MONITORING"
        const val NOTIFICATION_ID = 8011
        private const val TAG = "GuardianService"
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var pollJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val notification = buildPersistentNotification()
        
        val hasLoc = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val fgsType = if (hasLoc) {
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                }
                startForeground(NOTIFICATION_ID, notification, fgsType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground error: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_MONITORING -> {
                stopMonitoring()
                stopSelf()
            }
            else -> {
                startMonitoring()
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startMonitoring() {
        setupLocationUpdates()
        startDashboardCommandPolling()
    }

    @SuppressLint("MissingPermission")
    private fun setupLocationUpdates() {
        try {
            val profile = MobileChorApp.instance.repository.ownerProfile.value
            val intervalMs = (profile.trackingIntervalMinutes * 60 * 1000L).coerceAtLeast(30_000L)

            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, intervalMs)
                .setMinUpdateIntervalMillis(15_000L)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    MobileChorApp.instance.repository.updateLocation(
                        location.latitude,
                        location.longitude,
                        location.accuracy
                    )
                    checkGeofenceBreach(location)
                }
            }

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start location updates: ${e.message}")
        }
    }

    private fun checkGeofenceBreach(location: Location) {
        val profile = MobileChorApp.instance.repository.ownerProfile.value
        if (profile.isGeofenceEnabled) {
            val results = FloatArray(1)
            Location.distanceBetween(
                location.latitude,
                location.longitude,
                profile.geofenceLat,
                profile.geofenceLng,
                results
            )
            val distance = results[0]
            if (distance > profile.geofenceRadiusMeters) {
                // Device left safe zone!
                Log.w(TAG, "Geofence breached! Current distance: ${distance}m vs radius: ${profile.geofenceRadiusMeters}m")
                val captureIntent = Intent(this, CameraCaptureService::class.java).apply {
                    action = CameraCaptureService.ACTION_CAPTURE_PHOTO
                    putExtra("trigger", "Safe Zone Geofence Breach (${distance.toInt()}m from safe zone)")
                }
                startForegroundService(captureIntent)
            }
        }
    }

    private fun startDashboardCommandPolling() {
        pollJob?.cancel()
        pollJob = serviceScope.launch {
            var syncTick = 0
            while (isActive) {
                try {
                    val repo = MobileChorApp.instance.repository
                    if (syncTick % 7 == 0) {
                        repo.syncStateWithCloud()
                    }
                    syncTick++

                    val deviceId = repo.deviceState.value.deviceId
                    val url = "${repo.backendUrl}/api/device/commands?deviceId=$deviceId"

                    val request = Request.Builder().url(url).build()
                    val response = httpClient.newCall(request).execute()

                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrEmpty()) {
                            val mapType = object : TypeToken<Map<String, Any>>() {}.type
                            val data: Map<String, Any> = gson.fromJson(body, mapType)
                            val command = data["command"] as? String
                            if (!command.isNullOrEmpty()) {
                                handleRemoteCommand(command, data)
                            }
                        }
                    }
                    response.close()
                } catch (ignored: Exception) {
                }
                delay(3000) // Poll interval for responsive remote control
            }
        }
    }

    private fun handleRemoteCommand(command: String, data: Map<String, Any>) {
        val repo = MobileChorApp.instance.repository
        val commandId = data["id"] as? String ?: ""

        when (command) {
            "LOCK" -> {
                val customMsg = data["customMessage"] as? String
                val customPhone = data["emergencyPhone"] as? String
                val currentProfile = repo.ownerProfile.value
                if (!customMsg.isNullOrBlank() || !customPhone.isNullOrBlank()) {
                    repo.saveOwnerProfile(
                        currentProfile.copy(
                            customLostMessage = customMsg?.takeIf { it.isNotBlank() } ?: currentProfile.customLostMessage,
                            emergencyPhone = customPhone?.takeIf { it.isNotBlank() } ?: currentProfile.emergencyPhone
                        )
                    )
                }
                repo.setLostMode(true)
                val lockIntent = Intent(this, LostModeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(lockIntent)
                repo.acknowledgeCommand(commandId, "EXECUTED", "Device locked in Lost Mode")
            }
            "UNLOCK" -> {
                repo.setLostMode(false)
                repo.acknowledgeCommand(commandId, "EXECUTED", "Lost Mode deactivated")
            }
            "ALARM" -> {
                val alarmIntent = Intent(this, AlarmService::class.java).apply {
                    action = AlarmService.ACTION_START_ALARM
                }
                startService(alarmIntent)
                repo.acknowledgeCommand(commandId, "EXECUTED", "Emergency siren activated")
            }
            "STOP_ALARM" -> {
                val alarmIntent = Intent(this, AlarmService::class.java).apply {
                    action = AlarmService.ACTION_STOP_ALARM
                }
                startService(alarmIntent)
                repo.acknowledgeCommand(commandId, "EXECUTED", "Siren silenced")
            }
            "CAPTURE_PHOTO" -> {
                val facing = data["cameraFacing"] as? String ?: "front"
                val burst = data["burst"] as? Boolean ?: false
                val captureIntent = Intent(this, CameraCaptureService::class.java).apply {
                    action = if (burst) CameraCaptureService.ACTION_BURST_CAPTURE else CameraCaptureService.ACTION_CAPTURE_PHOTO
                    putExtra("cameraFacing", facing)
                    putExtra("trigger", "Owner Remote Demand Photo ($facing camera)")
                }
                startForegroundService(captureIntent)
                repo.acknowledgeCommand(commandId, "EXECUTED", "Captured $facing camera evidence")
            }
            "UPDATE_GEOFENCE" -> {
                val enabled = data["enabled"] as? Boolean ?: repo.ownerProfile.value.isGeofenceEnabled
                val lat = (data["latitude"] as? Number)?.toDouble() ?: repo.ownerProfile.value.geofenceLat
                val lng = (data["longitude"] as? Number)?.toDouble() ?: repo.ownerProfile.value.geofenceLng
                val radius = (data["radius"] as? Number)?.toFloat() ?: repo.ownerProfile.value.geofenceRadiusMeters
                repo.saveOwnerProfile(
                    repo.ownerProfile.value.copy(
                        isGeofenceEnabled = enabled,
                        geofenceLat = lat,
                        geofenceLng = lng,
                        geofenceRadiusMeters = radius
                    )
                )
                repo.acknowledgeCommand(commandId, "EXECUTED", "Safe Zone updated (${radius.toInt()}m)")
            }
            "SYNC_NOW" -> {
                repo.syncStateWithCloud()
                repo.acknowledgeCommand(commandId, "EXECUTED", "Telemetry ping refreshed")
            }
            "WIPE_SELECTIVE" -> {
                repo.evidenceRepository.getEvidenceDirectory().deleteRecursively()
                repo.acknowledgeCommand(commandId, "EXECUTED", "Evidence vault selectively wiped")
            }
            "WIPE_FULL" -> {
                repo.acknowledgeCommand(commandId, "EXECUTED", "Full device wipe initiated")
                val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                if (repo.isDeviceAdminActive()) {
                    try {
                        dpm?.wipeData(0)
                    } catch (e: Exception) {
                        Log.e(TAG, "Wipe failed: ${e.message}")
                    }
                }
            }
        }
    }

    private fun stopMonitoring() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        pollJob?.cancel()
        pollJob = null
    }

    private fun buildPersistentNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, MobileChorApp.CHANNEL_SERVICE)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.status_protected))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        stopMonitoring()
        super.onDestroy()
    }
}
