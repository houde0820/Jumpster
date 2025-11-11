package com.dp.jumpster.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class FireworksView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Particle(
        var angleRad: Float,
        var speed: Float,
        var radius: Float,
        var color: Int,
        var alpha: Int
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private var particles: List<Particle> = emptyList()
    private var progress: Float = 0f
    private var animator: ValueAnimator? = null
    private var centerX: Float? = null
    private var centerY: Float? = null

    fun startSmall() {
        startInternal(null, null, 40, 8f, 900L)
    }

    fun startBig() {
        startInternal(null, null, 140, 12f, 1500L)
    }

    fun startSmallAt(cx: Float, cy: Float) {
        startInternal(cx, cy, 40, 8f, 900L)
    }

    fun startBigAt(cx: Float, cy: Float) {
        startInternal(cx, cy, 140, 12f, 1500L)
    }

    private fun startInternal(
        cx: Float?,
        cy: Float?,
        particleCount: Int,
        maxRadius: Float,
        duration: Long
    ) {
        animator?.cancel()
        centerX = cx
        centerY = cy
        particles = buildParticles(particleCount, maxRadius)
        progress = 0f
        visibility = VISIBLE
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            interpolator = DecelerateInterpolator()
            this.duration = duration
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            doOnEnd { endAnimation() }
            start()
        }
    }

    private fun endAnimation() {
        visibility = GONE
        particles = emptyList()
        progress = 0f
        centerX = null
        centerY = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        animator = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (particles.isEmpty()) return
        val cx = centerX ?: (width / 2f)
        val cy = centerY ?: (height / 3f)
        particles.forEach { p ->
            val dist = p.speed * progress
            val x = cx + dist * cos(p.angleRad)
            val y = cy + dist * sin(p.angleRad)
            val a = (p.alpha * (1f - progress)).toInt().coerceIn(0, 255)
            paint.color = (a shl 24) or (p.color and 0x00FFFFFF)
            canvas.drawCircle(x, y, p.radius, paint)
        }
    }

    private fun buildParticles(count: Int, maxRadius: Float): List<Particle> {
        val colors = intArrayOf(
            0xFF00C853.toInt(),
            0xFFFF6D00.toInt(),
            0xFFBB86FC.toInt(),
            0xFF03A9F4.toInt()
        )
        val list = ArrayList<Particle>(count)
        val rand = Random(System.currentTimeMillis())
        repeat(count) {
            val angle = rand.nextFloat() * (Math.PI * 2).toFloat()
            val speed = 160f + rand.nextFloat() * 420f
            val radius = 3f + rand.nextFloat() * (maxRadius - 3f)
            val color = colors[rand.nextInt(colors.size)]
            val alpha = 230
            list.add(Particle(angle, speed, radius, color, alpha))
        }
        return list
    }
}

private fun ValueAnimator.doOnEnd(block: () -> Unit) {
    addListener(object : android.animation.AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: android.animation.Animator) {
            // 使用post确保在主线程执行清理
            block()
        }

        override fun onAnimationCancel(animation: android.animation.Animator) {
            // 动画被取消时也要执行清理
            block()
        }
    })
}
