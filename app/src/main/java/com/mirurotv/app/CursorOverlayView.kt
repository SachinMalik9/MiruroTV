package com.mirurotv.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class CursorOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Cursor position
    var cursorX: Float = 0f
        private set
    var cursorY: Float = 0f
        private set

    // Cursor appearance
    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val cursorBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val cursorShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 0, 0, 0)
        style = Paint.Style.FILL
    }

    // Cursor shape (arrow pointer)
    private val cursorPath = Path()
    private val shadowPath = Path()
    private val cursorSize = 28f

    // Movement speed (pixels per key press)
    var moveSpeed = 18f

    // Acceleration for held keys
    private var accelerationCounter = 0
    private val maxSpeed = 50f

    var isVisible = true
        set(value) {
            field = value
            invalidate()
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Start cursor at center of screen
        cursorX = w / 2f
        cursorY = h / 2f
    }

    fun moveCursor(dx: Float, dy: Float) {
        // Acceleration: move faster when key is held
        accelerationCounter++
        val speed = (moveSpeed + (accelerationCounter / 3f)).coerceAtMost(maxSpeed)

        cursorX = (cursorX + dx * speed).coerceIn(0f, width.toFloat())
        cursorY = (cursorY + dy * speed).coerceIn(0f, height.toFloat())
        invalidate()
    }

    fun resetAcceleration() {
        accelerationCounter = 0
    }

    fun setCursorPosition(x: Float, y: Float) {
        cursorX = x.coerceIn(0f, width.toFloat())
        cursorY = y.coerceIn(0f, height.toFloat())
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isVisible) return

        // Build arrow cursor shape
        cursorPath.reset()
        cursorPath.moveTo(cursorX, cursorY) // Tip
        cursorPath.lineTo(cursorX, cursorY + cursorSize) // Down
        cursorPath.lineTo(cursorX + cursorSize * 0.35f, cursorY + cursorSize * 0.75f) // Inner
        cursorPath.lineTo(cursorX + cursorSize * 0.55f, cursorY + cursorSize * 1.1f) // Tail right
        cursorPath.lineTo(cursorX + cursorSize * 0.7f, cursorY + cursorSize * 0.95f) // Tail right top
        cursorPath.lineTo(cursorX + cursorSize * 0.45f, cursorY + cursorSize * 0.6f) // Inner right
        cursorPath.lineTo(cursorX + cursorSize * 0.8f, cursorY + cursorSize * 0.6f) // Right point
        cursorPath.close()

        // Draw shadow (offset by 2px)
        canvas.save()
        canvas.translate(2f, 2f)
        shadowPath.reset()
        shadowPath.addPath(cursorPath, -2f + 2f, -2f + 2f)
        // Just draw shadow as offset cursor
        cursorPath.offset(0f, 0f, shadowPath)
        canvas.drawPath(cursorPath, cursorShadowPaint)
        canvas.restore()

        // Draw cursor
        canvas.drawPath(cursorPath, cursorPaint)
        canvas.drawPath(cursorPath, cursorBorderPaint)
    }
}
