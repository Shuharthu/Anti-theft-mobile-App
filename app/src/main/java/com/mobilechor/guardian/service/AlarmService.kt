package com.mobilechor.guardian.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.mobilechor.guardian.MobileChorApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AlarmService : Service() {

    companion object {
        const val ACTION_START_ALARM = "ACTION_START_ALARM"
        const val ACTION_STOP_ALARM = "ACTION_STOP_ALARM"
    }

    private var alarmJob: Job? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_ALARM -> startSiren()
            ACTION_STOP_ALARM -> stopSiren()
        }
        return START_NOT_STICKY
    }

    private fun startSiren() {
        stopSiren()
        MobileChorApp.instance.repository.setAlarmPlaying(true)

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

        alarmJob = CoroutineScope(Dispatchers.Default).launch {
            var toneGenerator: ToneGenerator? = null
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                val pattern = longArrayOf(0, 500, 200, 500)

                while (isActive) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(pattern, -1)
                    }

                    // Dual frequency emergency siren simulation
                    toneGenerator.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 700)
                    delay(800)
                    toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 700)
                    delay(800)
                }
            } catch (ignored: Exception) {
            } finally {
                toneGenerator?.release()
            }
        }
    }

    private fun stopSiren() {
        alarmJob?.cancel()
        alarmJob = null
        vibrator?.cancel()
        MobileChorApp.instance.repository.setAlarmPlaying(false)
        stopSelf()
    }

    override fun onDestroy() {
        stopSiren()
        super.onDestroy()
    }
}
