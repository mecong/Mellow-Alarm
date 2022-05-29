package com.mecong.tenderalarm.sleep_assistant

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView
import org.greenrobot.eventbus.EventBus

data class PlayPosition(val playPositionPercent: Float, val final: Boolean)

class PlayPositionTextView(context: Context, attributes: android.util.AttributeSet) :
  AppCompatTextView(context, attributes) {
  var interactiveMode = false


  private var playPositionPercent: Float = 0f
    set(value) {
      field = value
      postInvalidate()
    }

  fun setPlayPosition(pos: Float) {
    if (!touchActive)
      playPositionPercent = pos
  }


  private val caretColor = Paint()
  private val progressColor = Paint()
  private var touchActive = false
  private var halfOfHeight = this.height / 2f
  private var thirdOfHeight = this.height / 3f

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    halfOfHeight = this.height / 2f
    thirdOfHeight = this.height / 3f
  }

  override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas)

    if (interactiveMode) {
      val xx = playPositionPercent * this.width

      if (touchActive) {
        canvas?.drawRoundRect(0f, 0f, xx, this.height.toFloat(), 10f, 10f, caretColor)
      } else {
        canvas?.drawRoundRect(0f, 0f, xx, this.height.toFloat(), 10f, 10f, progressColor)
      }
    }
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    if (interactiveMode) {
      when (event.action) {
        MotionEvent.ACTION_UP -> {
          touchActive = false
          EventBus.getDefault().post(PlayPosition(playPositionPercent, true))

          invalidate()
        }
        MotionEvent.ACTION_DOWN -> {
          touchActive = true
          val eventX = when {
            (event.x > this.width) -> this.width.toFloat()
            (event.x < 0) -> 0f
            else -> event.x
          }

          playPositionPercent = eventX / this.width
          invalidate()
          EventBus.getDefault().post(PlayPosition(playPositionPercent, false))

        }
        MotionEvent.ACTION_MOVE -> {
          val eventX = when {
            (event.x > this.width) -> this.width.toFloat()
            (event.x < 0) -> 0f
            else -> event.x
          }

          playPositionPercent = eventX / this.width
          invalidate()
          EventBus.getDefault().post(PlayPosition(playPositionPercent, false))
        }
      }

    }

    return super.onTouchEvent(event)
  }

  init {
    caretColor.color = Color.parseColor("#33327C1F")
    progressColor.color = Color.parseColor("#33E9DEDE")
  }
}