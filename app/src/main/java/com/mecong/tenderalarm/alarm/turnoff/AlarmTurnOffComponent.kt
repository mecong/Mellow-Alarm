package com.mecong.tenderalarm.alarm.turnoff

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.mecong.tenderalarm.alarm.AlarmMessage
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.math.min
import kotlin.math.sqrt

class AlarmTurnOffComponent(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    var complexity = 0
    private var lockingPaint = Paint()
    private var figures: Array<DraggableCircle?>? = null
    private var activeFigure: Int? = null
    private var viewPortBoundsForEvent: Rect? = null
    private var prevX = 0
    private var prevY = 0
    private var draggableCirclePaints: Array<Paint?> = arrayOfNulls(50)
    private var spiritPaints: Array<Paint?> = arrayOfNulls(10)
    private lateinit var positions: MutableList<Point>
    private var minCircleRadius = 90
    private var circleStep = 20

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (oldw != 0 || oldh != 0) {
            val viewPortBounds = Rect(paddingLeft, paddingTop,
                    this.right - paddingRight, this.height - paddingBottom)
            viewPortBoundsForEvent = Rect(paddingLeft, paddingTop,
                    this.width - paddingRight, this.height - paddingBottom)
            viewPortBoundsForEvent!!.inset(50, 50)


//            val maxRadius = minCircleRadius + circleStep * (MAX_COMPLEXITY - 1)
//            var maxDiameter = maxRadius * 2
            val square = viewPortBounds.width() * viewPortBounds.height()
//            square/(maxDiameter*maxDiameter)= MAX_COMPLEXITY*2
            val maxDiameter = sqrt(square / (MAX_COMPLEXITY * 2.0))
            minCircleRadius = ((maxDiameter / 2 - circleStep * (MAX_COMPLEXITY - 1)) * 0.9).toInt()


            positions = calculatePositions(viewPortBounds)
            figures = arrayOfNulls(min(positions.size / 2, complexity))

            for (i in figures!!.indices.reversed()) {
                figures!![i] = DraggableCircle(positions, minCircleRadius + circleStep * (MAX_COMPLEXITY - 1 - i))
            }
        }
    }

    private fun calculatePositions(viewPortBounds: Rect): MutableList<Point> {
        val maxRadius = minCircleRadius + circleStep * (MAX_COMPLEXITY - 1)
        val diameter = maxRadius * 2
        val maxVertical = viewPortBounds.height() / diameter
        val maxHorizontal = viewPortBounds.width() / diameter
        val spaceHorizontal = (viewPortBounds.width() - maxHorizontal * diameter) / maxHorizontal
        val spaceVertical = (viewPortBounds.height() - maxVertical * diameter) / maxVertical
        positions = ArrayList()

        for (i in 1..maxHorizontal) {
            for (j in 1..maxVertical) {
                val point = Point(i * (diameter + spaceHorizontal) - maxRadius,
                        j * (diameter + spaceVertical) - maxRadius)
                positions.add(point)
            }
        }

        positions.shuffle()
        return positions
    }

    init {
        val effect = DashPathEffect(floatArrayOf(10f, 20f), 0f)

        spiritPaints[0] = Paint()
        spiritPaints[0]!!.color = Color.rgb(30, 86, 49)
        spiritPaints[0]!!.style = Paint.Style.STROKE
        spiritPaints[0]!!.strokeWidth = STROKE_WIDTH.toFloat()
        spiritPaints[0]!!.pathEffect = effect

        spiritPaints[1] = Paint()
        spiritPaints[1]!!.color = Color.rgb(164, 222, 2)
        spiritPaints[1]!!.style = Paint.Style.STROKE
        spiritPaints[1]!!.strokeWidth = STROKE_WIDTH.toFloat()
        spiritPaints[1]!!.pathEffect = effect

        spiritPaints[2] = Paint()
        spiritPaints[2]!!.color = Color.rgb(118, 186, 27)
        spiritPaints[2]!!.style = Paint.Style.STROKE
        spiritPaints[2]!!.strokeWidth = STROKE_WIDTH.toFloat()
        spiritPaints[2]!!.pathEffect = effect

        spiritPaints[3] = Paint()
        spiritPaints[3]!!.color = Color.rgb(76, 154, 42)
        spiritPaints[3]!!.style = Paint.Style.STROKE
        spiritPaints[3]!!.strokeWidth = STROKE_WIDTH.toFloat()
        spiritPaints[3]!!.pathEffect = effect

        spiritPaints[4] = Paint()
        spiritPaints[4]!!.color = Color.rgb(104, 187, 89)
        spiritPaints[4]!!.style = Paint.Style.STROKE
        spiritPaints[4]!!.strokeWidth = STROKE_WIDTH.toFloat()
        spiritPaints[4]!!.pathEffect = effect

        for (i in 5 until spiritPaints.size) {
            spiritPaints[i] = Paint()
            spiritPaints[i]!!.color = Color.rgb(49, 99, 0)
            spiritPaints[i]!!.style = Paint.Style.STROKE
            spiritPaints[i]!!.strokeWidth = STROKE_WIDTH.toFloat()
            spiritPaints[i]!!.pathEffect = effect
        }
    }

    init {
        draggableCirclePaints[0] = Paint()
        draggableCirclePaints[0]!!.color = Color.rgb(30, 86, 49)
        draggableCirclePaints[0]!!.style = Paint.Style.FILL

        draggableCirclePaints[1] = Paint()
        draggableCirclePaints[1]!!.color = Color.rgb(164, 222, 2)
        draggableCirclePaints[1]!!.style = Paint.Style.FILL

        draggableCirclePaints[2] = Paint()
        draggableCirclePaints[2]!!.color = Color.rgb(118, 186, 27)
        draggableCirclePaints[2]!!.style = Paint.Style.FILL

        draggableCirclePaints[3] = Paint()
        draggableCirclePaints[3]!!.color = Color.rgb(76, 154, 42)
        draggableCirclePaints[3]!!.style = Paint.Style.FILL

        draggableCirclePaints[4] = Paint()
        draggableCirclePaints[4]!!.color = Color.rgb(104, 187, 89)
        draggableCirclePaints[4]!!.style = Paint.Style.FILL

        for (i in 5 until draggableCirclePaints.size) {
            draggableCirclePaints[i] = Paint()
            draggableCirclePaints[i]!!.color = Color.rgb(49, 99, 0)
            draggableCirclePaints[i]!!.style = Paint.Style.FILL
        }
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (figures == null) return
        var amountOfFixed = 0
        //        if (BuildConfig.DEBUG) {
//            if (lastEvent != null) {
//                canvas.drawText(String.format("%f %f", lastEvent.getX(), lastEvent.getY()), 50, 50, lockingPaint);
//                canvas.drawText(String.format("%f %f", lastEvent.getRawX(), lastEvent.getRawY()), 50, 100, lockingPaint);
//                canvas.drawText(String.format("%d %d %d %d", viewPortBounds.left, viewPortBounds.top, viewPortBounds.right, viewPortBounds.bottom), 50, 200, lockingPaint);
//                canvas.drawText(String.format("%d %d %d %d", viewPortBoundsForEvent.left, viewPortBoundsForEvent.top, viewPortBoundsForEvent.right, viewPortBoundsForEvent.bottom), 50, 300, lockingPaint);
//                canvas.drawText(String.format("%d %d", this.getLeft(), this.getTop()), 50, 400, lockingPaint);
//            }
//
//
////            for (int i = 0; i < positions.size(); i++) {
////                final Point point = positions.get(i);
////                canvas.drawCircle(point.x, point.y, 80, lockingPaint);
////            }
//        }
        for (i in figures!!.indices.reversed()) {
            val figure = figures!![i]
            if (figure!!.isFixed) {
                canvas.drawCircle(figure.aimPoint.x.toFloat(), figure.aimPoint.y.toFloat(), figure.radius.toFloat(), lockingPaint)
                amountOfFixed++
            }
        }

        for (i in figures!!.indices.reversed()) {
            val figure = figures!![i]
            if (!figure!!.isFixed) {
                canvas.drawCircle(figure.aimPoint.x.toFloat(), figure.aimPoint.y.toFloat(), figure.radius.toFloat(), spiritPaints[i]!!)
                canvas.drawCircle(figure.currentPoint.x.toFloat(), figure.currentPoint.y.toFloat(), figure.radius.toFloat(), draggableCirclePaints[i]!!)
            }
        }

        if (amountOfFixed == 2) {
            EventBus.getDefault().post(AlarmMessage.CANCEL_VOLUME_INCREASE)
        }

        if (amountOfFixed >= figures!!.size) {
            EventBus.getDefault().postSticky(AlarmMessage.STOP_ALARM)
            //            getContext().stopService(new Intent(getContext(), AlarmNotifyingService.class));
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var result = super.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_UP) {
            activeFigure = null
            invalidate()
            result = true
        } else {
            val x = event.x.toInt()
            val y = event.y.toInt()
            if (event.action == MotionEvent.ACTION_DOWN) {
                activeFigure = findFigure(event)
                if (activeFigure != null) {
                    prevX = x
                    prevY = y
                    invalidate()
                    result = true
                }
            } else if (event.action == MotionEvent.ACTION_MOVE && activeFigure != null && viewPortBoundsForEvent!!.contains(x, y)) {
                figures!![activeFigure!!]!!.offset(x - prevX, y - prevY)
                prevX = x
                prevY = y
                invalidate()
                result = true
            }
        }
        invalidate()
        return result
    }

    private fun findFigure(event: MotionEvent): Int? {
        for (i in figures!!.indices) {
            if (figures!![i]!!.isInFigure(event.x, event.y)) {
                return i
            }
        }
        return null
    }

    companion object {
        const val STROKE_WIDTH = 9
        const val MAX_COMPLEXITY = 6
    }

    init {
        lockingPaint.color = Color.DKGRAY
        lockingPaint.textSize = 50f
        lockingPaint.style = Paint.Style.FILL_AND_STROKE
    }
}