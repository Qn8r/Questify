package com.example.livinglifemmo

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object SoundManager {
    private var soundPool: SoundPool? = null
    private var enabled: Boolean = true
    private var hapticsEnabled: Boolean = true
    private var vibrator: Vibrator? = null

    // Sound IDs (default to -1, which means "not loaded")
    private var clickId: Int = -1
    private var successId: Int = -1
    private var acceptId: Int = -1
    private var levelUpId: Int = -1

    fun init(context: Context) {
        if (vibrator == null) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }
        if (soundPool == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build()

            // LOAD CUSTOM SOUNDS (Place these in res/raw/)
            // If the file is missing, it returns -1, and we just won't play anything.
            clickId = loadSound(context, "mmo_click")
            successId = loadSound(context, "mmo_success")
            acceptId = loadSound(context, "mmo_accept")
            levelUpId = loadSound(context, "mmo_levelup")
        }
    }

    fun setEnabled(value: Boolean) {
        enabled = value
    }

    fun setHapticsEnabled(value: Boolean) {
        hapticsEnabled = value
    }

    private fun vibrate(durationMs: Long) {
        if (!hapticsEnabled) return
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(durationMs)
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun loadSound(context: Context, resName: String): Int {
        val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
        return if (resId != 0) {
            soundPool?.load(context, resId, 1) ?: -1
        } else {
            -1 // File not found -> Silent
        }
    }

    // Standard UI Click (Navigation)
    fun playClick() {
        if (enabled && clickId != -1) {
            soundPool?.play(clickId, 1f, 1f, 1, 0, 1f)
        }
        vibrate(12L)
        // NO ELSE: If file is missing, remain silent.
    }

    // "Congratz" / Reward Claimed
    fun playSuccess() {
        if (enabled && successId != -1) {
            soundPool?.play(successId, 1f, 1f, 1, 0, 1f)
        }
        vibrate(24L)
    }

    // Quest Started / Wizard Accepted
    fun playAccept() {
        if (enabled && acceptId != -1) {
            soundPool?.play(acceptId, 1f, 1f, 1, 0, 1f)
        }
        vibrate(18L)
    }

    // Level Up Event
    fun playLevelUp() {
        if (enabled && levelUpId != -1) {
            soundPool?.play(levelUpId, 1f, 1f, 1, 0, 1f)
        }
        vibrate(30L)
    }
}
