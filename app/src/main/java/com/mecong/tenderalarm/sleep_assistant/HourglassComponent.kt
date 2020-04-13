package com.mecong.tenderalarm.sleep_assistant

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.mecong.tenderalarm.R
import java.util.*
import kotlin.math.max
import kotlin.math.min

interface SleepTimerViewValueListener {
    fun onValueChanged(newValue: Long)
}

class HourglassComponent(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var max: Long = 0
    private var currentValue: Long = 0
    private var changingMode = false
    private val filledPaint = Paint()
    private val framedPaint = Paint()
    private val changingPaint = Paint()
    private val listeners: MutableList<SleepTimerViewValueListener> = ArrayList(1)
    private lateinit var frame: Rect
    private var frameHeightDivMax = 0f
    private var maxDivFrameHeight = 0f

    fun addListener(listener: SleepTimerViewValueListener) {
        listeners.add(listener)
    }

    fun setCurrentValue(currentValue: Long) {
        this.currentValue = currentValue
        invalidate()
        requestLayout()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        frame = bounds()
        frameHeightDivMax = frame.height().toFloat() / max
        maxDivFrameHeight = max / frame.height().toFloat()
    }

    private fun bounds(): Rect {
        return Rect(paddingLeft, paddingTop,
                width - paddingRight, height - paddingBottom)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val padding = 10f
        val filled = (frame.height() + padding) - frameHeightDivMax * currentValue
        //        canvas.drawRoundRect(frame.left, filled, frame.right, frame.bottom,25f,25f, filledPaint);
        canvas.drawRect(frame.left + padding,
                filled,
                frame.right - padding,
                frame.bottom.toFloat(),
                filledPaint)
        //        canvas.drawRect(frame, framedPaint);
//        if (changingMode) {
////            canvas.drawLine(frame.left, filled, frame.right, filled, changingPaint);
////            canvas.drawCircle(0, filled, 5, changingPaint);
////            canvas.drawCircle(frame.width(), filled, 5, changingPaint);
//        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        performClick()
        var result = false
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                changingMode = false
                invalidate()
                result = true
            }
            MotionEvent.ACTION_DOWN -> {
                changingMode = true
                invalidate()
                result = true
            }
            MotionEvent.ACTION_MOVE -> {
                var newValue = ((frame.height() - event.y) * maxDivFrameHeight).toLong()
                newValue = min(newValue, max)
                newValue = max(newValue, 5)
                currentValue = newValue
                for (listener in listeners) {
                    listener.onValueChanged(currentValue)
                }
                invalidate()
                requestLayout()
                result = true
            }
        }
        return result
    }


    init {
        val a = context.theme.obtainStyledAttributes(attrs,
                R.styleable.HourglassComponent, 0, 0)
        try {
            max = a.getInteger(R.styleable.HourglassComponent_max, 100).toLong()
            currentValue = a.getInteger(R.styleable.HourglassComponent_currentValue, 30).toLong()
        } finally {
            a.recycle()
        }

        filledPaint.color = Color.parseColor("#323030")

        framedPaint.color = filledPaint.color
        framedPaint.style = Paint.Style.STROKE
        framedPaint.strokeWidth = 1f

        changingPaint.color = Color.GRAY
        changingPaint.strokeWidth = 5f

        //TODO: remove?
        for (listener in listeners) {
            listener.onValueChanged(currentValue)
        }
    }
}