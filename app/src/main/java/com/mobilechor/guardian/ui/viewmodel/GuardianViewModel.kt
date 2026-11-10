package com.mobilechor.guardian.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobilechor.guardian.MobileChorApp
import com.mobilechor.guardian.data.model.DeviceState
import com.mobilechor.guardian.data.model.EvidenceRecord
import com.mobilechor.guardian.data.model.OwnerProfile
import com.mobilechor.guardian.service.AlarmService
import com.mobilechor.guardian.service.CameraCaptureService
import com.mobilechor.guardian.service.GuardianService
import com.mobilechor.guardian.ui.screens.LostModeActivity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class GuardianViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MobileChorApp.instance.repository
    private val evidenceRepo = repository.evidenceRepository

    val deviceState: StateFlow<DeviceState> = repository.deviceState
    val ownerProfile: StateFlow<OwnerProfile> = repository.ownerProfile
    val evidenceList: StateFlow<List<EvidenceRecord>> = evidenceRepo.evidenceList
    val failedPinCount: StateFlow<Int> = repository.failedPinCount

    init {
        // Ensure background protection service is started
        val serviceIntent = Intent(application, GuardianService::class.java)
        application.startForegroundService(serviceIntent)
        repository.updateAdminStatus()
    }

    fun saveOwnerProfile(profile: OwnerProfile) {
        repository.saveOwnerProfile(profile)
    }

    fun completeOnboarding(profile: OwnerProfile, pin: String) {
        val hashed = com.mobilechor.guardian.util.SecurityUtils.hashPin(pin)
        val updated = profile.copy(
            pinHash = hashed,
            isOnboardingComplete = true
        )
        repository.saveOwnerProfile(updated)
    }

    fun verifyPin(pin: String): Boolean {
        val result = repository.verifyPin(pin)
        if (!result && repository.failedPinCount.value >= 2) {
            // Trigger 2-strike front-camera intruder capture
            val captureIntent = Intent(getApplication(), CameraCaptureService::class.java).apply {
                action = CameraCaptureService.ACTION_CAPTURE_PHOTO
                putExtra("trigger", "2 Failed In-App PIN Attempts")
            }
            getApplication<Application>().startForegroundService(captureIntent)
        }
        return result
    }

    fun triggerAlarm() {
        val alarmIntent = Intent(getApplication(), AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
        }
        getApplication<Application>().startService(alarmIntent)
    }

    fun stopAlarm() {
        val alarmIntent = Intent(getApplication(), AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
        }
        getApplication<Application>().startService(alarmIntent)
    }

    fun toggleLostMode(enabled: Boolean) {
        repository.setLostMode(enabled)
        if (enabled) {
            val lostIntent = Intent(getApplication(), LostModeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            getApplication<Application>().startActivity(lostIntent)
        }
    }

    fun triggerTestPhotoCapture() {
        val captureIntent = Intent(getApplication(), CameraCaptureService::class.java).apply {
            action = CameraCaptureService.ACTION_CAPTURE_PHOTO
            putExtra("trigger", "Owner Manual Test Photo Capture")
        }
        getApplication<Application>().startForegroundService(captureIntent)
    }

    fun deleteEvidence(recordId: String) {
        evidenceRepo.deleteEvidence(recordId)
    }

    fun setLanguage(context: Context, langCode: String) {
        val currentProfile = repository.ownerProfile.value
        repository.saveOwnerProfile(currentProfile.copy(language = langCode))

        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun updateTrackingInterval(minutes: Int) {
        val current = repository.ownerProfile.value
        repository.saveOwnerProfile(current.copy(trackingIntervalMinutes = minutes))
    }

    fun toggleGeofencing(enabled: Boolean) {
        val current = repository.ownerProfile.value
        repository.saveOwnerProfile(current.copy(isGeofenceEnabled = enabled))
    }

    fun toggleAudioRecording(enabled: Boolean) {
        val current = repository.ownerProfile.value
        repository.saveOwnerProfile(current.copy(isAudioRecordingOptIn = enabled))
    }

    fun toggleBiometrics(enabled: Boolean) {
        val current = repository.ownerProfile.value
        repository.saveOwnerProfile(current.copy(isBiometricEnabled = enabled))
    }

    fun refreshTelemetry() {
        repository.updateAdminStatus()
        repository.syncStateWithCloud()
    }
}
