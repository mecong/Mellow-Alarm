package com.mecong.tenderalarm.alarm.turnoff

import android.graphics.Point
import android.graphics.Rect
import java.util.*

internal class DraggableCircle(positions: MutableList<Point>, var radius: Int) {
    var isFixed = false
    private var random = Random()
    private var proximity: Rect? = null
    lateinit var currentPoint: Point
    lateinit var aimPoint: Point

    fun isInFigure(x: Float, y: Float): Boolean {
        return x < currentPoint.x + radius && x > currentPoint.x - radius && y < currentPoint.y + radius && y > currentPoint.y - radius
    }

    fun offset(x: Int, y: Int) {
        currentPoint.offset(x, y)
        if (proximity!!.contains(currentPoint.x, currentPoint.y)) {
            currentPoint[aimPoint.x] = aimPoint.y
            isFixed = true
        }
    }

    private fun generateRandomCircle(positions: MutableList<Point>) {
        val displacementX = random.nextInt(MAX_DISPLACEMENT / 2) - random.nextInt(MAX_DISPLACEMENT)
        val displacementY = random.nextInt(MAX_DISPLACEMENT / 2) - random.nextInt(MAX_DISPLACEMENT)
        aimPoint = positions.removeAt(0)
        aimPoint.offset(displacementX, displacementY)
        currentPoint = positions.removeAt(0)
        currentPoint.offset(-displacementX, -displacementY)
        proximity = Rect(
                aimPoint.x - AIM_DISTANCE,
                aimPoint.y - AIM_DISTANCE,
                aimPoint.x + AIM_DISTANCE,
                aimPoint.y + AIM_DISTANCE)
    }

    companion object {
        private const val AIM_DISTANCE = 15
        private const val MAX_DISPLACEMENT = 60
    }

    init {
        generateRandomCircle(positions)
    }
}