package com.example.speakeasy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class WaveformView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.parseColor("#6200EE") // Primary Color
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val amplitudes = mutableListOf<Float>()
    private val maxBars = 50 // Number of bars to show
    private val barWidth = 10f
    private val barGap = 5f

    fun addAmplitude(amplitude: Float) {
        // Normalize amplitude (usually -2 to 10 from SpeechRecognizer) to a 0-100 scale
        val normalized = ((amplitude + 2) * 10).coerceIn(5f, 100f)
        
        amplitudes.add(normalized)
        if (amplitudes.size > maxBars) {
            amplitudes.removeAt(0)
        }
        invalidate() // Trigger redraw
    }
    
    fun clear() {
        amplitudes.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerY = height / 2f
        var startX = width - (amplitudes.size * (barWidth + barGap))

        for (amp in amplitudes) {
            // Calculate bar height based on amplitude percentage of view height
            val barHeight = (amp / 100f) * height * 0.8f 
            
            val left = startX
            val top = centerY - (barHeight / 2)
            val right = left + barWidth
            val bottom = centerY + (barHeight / 2)
            
            // Draw rounded rectangle
            canvas.drawRoundRect(left, top, right, bottom, 5f, 5f, paint)
            
            startX += barWidth + barGap
        }
    }
}
