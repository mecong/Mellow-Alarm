package com.mecong.tenderalarm.alarm.turnoff;


import android.graphics.Point;
import android.graphics.Rect;

import java.util.List;
import java.util.Random;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;


@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
class DraggableCircle {

    private static final int AIM_DISTANCE = 15;
    private static final int MAX_DISPLACEMENT = 60;

    int radius;
    @Setter
    boolean fixed = false;
    Random random = new Random();
    Rect proximity;
    Point currentPoint, aimPoint;

    DraggableCircle(List<Point> positions, int circleRadius) {
        this.radius = circleRadius;
        generateRandomCircle(positions);
    }

    boolean isInFigure(float x, float y) {
        return x < currentPoint.x + radius && x > currentPoint.x - radius
                && y < currentPoint.y + radius && y > currentPoint.y - radius;
    }

    void offset(int x, int y) {
        currentPoint.offset(x, y);

        if (proximity.contains(currentPoint.x, currentPoint.y)) {
            currentPoint.set(aimPoint.x, aimPoint.y);
            fixed = true;
        }
    }

    private void generateRandomCircle(List<Point> positions) {
        int displacementX = random.nextInt(MAX_DISPLACEMENT / 2) - random.nextInt(MAX_DISPLACEMENT);
        int displacementY = random.nextInt(MAX_DISPLACEMENT / 2) - random.nextInt(MAX_DISPLACEMENT);

        aimPoint = positions.remove(0);
        aimPoint.offset(displacementX, displacementY);

        currentPoint = positions.remove(0);
        currentPoint.offset(-displacementX, -displacementY);

        proximity = new Rect(
                aimPoint.x - AIM_DISTANCE,
                aimPoint.y - AIM_DISTANCE,
                aimPoint.x + AIM_DISTANCE,
                aimPoint.y + AIM_DISTANCE);
    }
}