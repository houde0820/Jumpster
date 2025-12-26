package com.dp.jumpster.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Perform haptic feedback or vibration for a view click.
 */
fun View.vibrateClick() {
    // 优先使用系统触觉反馈（无需权限）
    performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)

    // 作为备选，使用振动反馈（如果权限允许）
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15)
            }
        }
    } catch (_: SecurityException) {
        // 无权限时忽略，系统触觉反馈已足够
    }
}
