package com.leonardovictormuller.napstop.data.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.leonardovictormuller.napstop.domain.alert.AlertFeedbackPlayer

/**
 * Android implementation of [AlertFeedbackPlayer].
 *
 * Handles physical vibration through [Vibrator] / [VibratorManager] and
 * audible alarm playback using default alarm ringtone with [AudioAttributes.USAGE_ALARM]
 * so that sound plays even if the device media/ringer volume is low or in quiet mode.
 */
class AndroidAlertFeedbackPlayer(
    private val context: Context,
) : AlertFeedbackPlayer {

    private val appContext = context.applicationContext

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private var isAlarmPlaying = false

    override fun triggerVibration() {
        stopAll()
        vibrator?.let { vib ->
            if (vib.hasVibrator()) {
                // Pattern: 0ms wait, 500ms vibrate, 300ms pause, 500ms vibrate, 300ms pause, 500ms vibrate
                val timings = longArrayOf(0, 500, 300, 500, 300, 500)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vib.vibrate(effect)
            }
        }
    }

    override fun triggerAlarm() {
        if (isAlarmPlaying) return
        stopAll()
        isAlarmPlaying = true

        // 1. Continuous vibration: repeat index 1 (timings[1], timings[2])
        vibrator?.let { vib ->
            if (vib.hasVibrator()) {
                val timings = longArrayOf(0, 800, 400)
                val effect = VibrationEffect.createWaveform(timings, 1)
                vib.vibrate(effect)
            }
        }

        // 2. Audible alarm using default alarm ringtone and USAGE_ALARM
        try {
            var alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(appContext, alarmUri)
                setAudioAttributes(audioAttributes)
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {
            // Safe fallback if audio hardware or ringtone is unavailable
        }
    }

    override fun stopAll() {
        isAlarmPlaying = false
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}

        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
    }
}
