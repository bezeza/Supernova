package com.supernova.launcher.fork.categories

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import kotlin.random.Random

// The tiles' gentle shake while they are being arranged. It is skipped when
// the system has animations off.
internal class Wobble(private val view: View) {

    private var animator: Animator? = null

    fun start() {
        if (animator != null || !ValueAnimator.areAnimatorsEnabled()) return
        val angle = ANGLE + Random.nextFloat() * ANGLE_SPREAD
        animator = ObjectAnimator.ofFloat(view, View.ROTATION, -angle, angle).apply {
            duration = DURATION_MS + Random.nextLong(DURATION_SPREAD_MS)
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    fun stop() {
        animator?.cancel()
        animator = null
        view.rotation = 0f
    }

    private companion object {
        const val ANGLE = 0.8f
        const val ANGLE_SPREAD = 0.5f
        const val DURATION_MS = 110L
        const val DURATION_SPREAD_MS = 50L
    }
}
