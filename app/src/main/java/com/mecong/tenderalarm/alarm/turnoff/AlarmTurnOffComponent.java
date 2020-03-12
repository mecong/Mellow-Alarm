package com.mecong.tenderalarm.alarm.turnoff;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.mecong.tenderalarm.alarm.AlarmMessage;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlarmTurnOffComponent extends View {
    public static final int STROKE_WIDTH = 9;
    public static final int MAX_COMPLEXITY = 6;
//    @Setter
    int complexity;
    Paint lockingPaint = new Paint();
    DraggableCircle[] figures;
    Integer activeFigure = null;
    Rect viewPortBoundsForEvent;
    int prevX, prevY;
    Paint[] draggableCirclePaints;
    Paint[] spiritPaints;
    List<Point> positions;


    public int getComplexity() {
        return complexity;
    }

    public void setComplexity(int complexity) {
        this.complexity = complexity;
    }

    public Paint getLockingPaint() {
        return lockingPaint;
    }

    public void setLockingPaint(Paint lockingPaint) {
        this.lockingPaint = lockingPaint;
    }

    public DraggableCircle[] getFigures() {
        return figures;
    }

    public void setFigures(DraggableCircle[] figures) {
        this.figures = figures;
    }

    public Integer getActiveFigure() {
        return activeFigure;
    }

    public void setActiveFigure(Integer activeFigure) {
        this.activeFigure = activeFigure;
    }

    public Rect getViewPortBoundsForEvent() {
        return viewPortBoundsForEvent;
    }

    public void setViewPortBoundsForEvent(Rect viewPortBoundsForEvent) {
        this.viewPortBoundsForEvent = viewPortBoundsForEvent;
    }

    public int getPrevX() {
        return prevX;
    }

    public void setPrevX(int prevX) {
        this.prevX = prevX;
    }

    public int getPrevY() {
        return prevY;
    }

    public void setPrevY(int prevY) {
        this.prevY = prevY;
    }

    public Paint[] getDraggableCirclePaints() {
        return draggableCirclePaints;
    }

    public void setDraggableCirclePaints(Paint[] draggableCirclePaints) {
        this.draggableCirclePaints = draggableCirclePaints;
    }

    public Paint[] getSpiritPaints() {
        return spiritPaints;
    }

    public void setSpiritPaints(Paint[] spiritPaints) {
        this.spiritPaints = spiritPaints;
    }

    public List<Point> getPositions() {
        return positions;
    }

    public void setPositions(List<Point> positions) {
        this.positions = positions;
    }

    public AlarmTurnOffComponent(Context context, AttributeSet attrs) {
        super(context, attrs);

        initDraggableCirclePaints();
        initSpiritPaints();

        lockingPaint.setColor(Color.DKGRAY);
        lockingPaint.setTextSize(50);
        lockingPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (oldw != 0 || oldh != 0) {
            figures = new DraggableCircle[this.complexity];

            Rect viewPortBounds = new Rect(getPaddingLeft(), getPaddingTop(),
                    this.getRight() - getPaddingRight(), this.getHeight() - getPaddingBottom());

            viewPortBoundsForEvent = new Rect(getPaddingLeft(), getPaddingTop(),
                    this.getWidth() - getPaddingRight(), this.getHeight() - getPaddingBottom());
            viewPortBoundsForEvent.inset(50, 50);

            int maxRadius = 90 + (20 * (MAX_COMPLEXITY - 1));
            positions = calculatePositions(maxRadius, viewPortBounds);

            for (int i = figures.length - 1; i >= 0; i--) {
                figures[i] = new DraggableCircle(positions, 90 + (20 * (MAX_COMPLEXITY - 1 - i)));
            }
        }
    }

    private List<Point> calculatePositions(int maxRadius, Rect viewPortBounds) {
        positions = new ArrayList<>();
        int diameter = maxRadius * 2;
        int maxVertical = viewPortBounds.height() / diameter;
        int maxHorizontal = viewPortBounds.width() / diameter;
        int spaceHorizontal = (viewPortBounds.width() - maxHorizontal * diameter) / maxHorizontal;
        int spaceVertical = (viewPortBounds.height() - maxVertical * diameter) / maxVertical;

        for (int i = 1; i <= maxHorizontal; i++) {
            for (int j = 1; j <= maxVertical; j++) {
                Point point = new Point(i * (diameter + spaceHorizontal) - maxRadius,
                        j * (diameter + spaceVertical) - maxRadius);
                positions.add(point);
            }
        }

        Collections.shuffle(positions);
        return positions;
    }

    private void initSpiritPaints() {
        spiritPaints = new Paint[10];
        DashPathEffect effect = new DashPathEffect(new float[]{10, 20}, 0);

        spiritPaints[0] = new Paint();
        spiritPaints[0].setColor(Color.rgb(30, 86, 49));
        spiritPaints[0].setStyle(Paint.Style.STROKE);
        spiritPaints[0].setStrokeWidth(STROKE_WIDTH);
        spiritPaints[0].setPathEffect(effect);

        spiritPaints[1] = new Paint();
        spiritPaints[1].setColor(Color.rgb(164, 222, 2));
        spiritPaints[1].setStyle(Paint.Style.STROKE);
        spiritPaints[1].setStrokeWidth(STROKE_WIDTH);
        spiritPaints[1].setPathEffect(effect);

        spiritPaints[2] = new Paint();
        spiritPaints[2].setColor(Color.rgb(118, 186, 27));
        spiritPaints[2].setStyle(Paint.Style.STROKE);
        spiritPaints[2].setStrokeWidth(STROKE_WIDTH);
        spiritPaints[2].setPathEffect(effect);

        spiritPaints[3] = new Paint();
        spiritPaints[3].setColor(Color.rgb(76, 154, 42));
        spiritPaints[3].setStyle(Paint.Style.STROKE);
        spiritPaints[3].setStrokeWidth(STROKE_WIDTH);
        spiritPaints[3].setPathEffect(effect);

        spiritPaints[4] = new Paint();
        spiritPaints[4].setColor(Color.rgb(104, 187, 89));
        spiritPaints[4].setStyle(Paint.Style.STROKE);
        spiritPaints[4].setStrokeWidth(STROKE_WIDTH);
        spiritPaints[4].setPathEffect(effect);

        for (int i = 5; i < spiritPaints.length; i++) {
            spiritPaints[i] = new Paint();
            spiritPaints[i].setColor(Color.rgb(49, 99, 0));
            spiritPaints[i].setStyle(Paint.Style.STROKE);
            spiritPaints[i].setStrokeWidth(STROKE_WIDTH);
            spiritPaints[i].setPathEffect(effect);
        }
    }

    private void initDraggableCirclePaints() {
        draggableCirclePaints = new Paint[50];

        draggableCirclePaints[0] = new Paint();
        draggableCirclePaints[0].setColor(Color.rgb(30, 86, 49));
        draggableCirclePaints[0].setStyle(Paint.Style.FILL);

        draggableCirclePaints[1] = new Paint();
        draggableCirclePaints[1].setColor(Color.rgb(164, 222, 2));
        draggableCirclePaints[1].setStyle(Paint.Style.FILL);

        draggableCirclePaints[2] = new Paint();
        draggableCirclePaints[2].setColor(Color.rgb(118, 186, 27));
        draggableCirclePaints[2].setStyle(Paint.Style.FILL);

        draggableCirclePaints[3] = new Paint();
        draggableCirclePaints[3].setColor(Color.rgb(76, 154, 42));
        draggableCirclePaints[3].setStyle(Paint.Style.FILL);

        draggableCirclePaints[4] = new Paint();
        draggableCirclePaints[4].setColor(Color.rgb(104, 187, 89));
        draggableCirclePaints[4].setStyle(Paint.Style.FILL);

        for (int i = 5; i < draggableCirclePaints.length; i++) {
            draggableCirclePaints[i] = new Paint();
            draggableCirclePaints[i].setColor(Color.rgb(49, 99, 0));
            draggableCirclePaints[i].setStyle(Paint.Style.FILL);
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (figures == null) return;

        int amountOfFixed = 0;

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


        for (int i = figures.length - 1; i >= 0; i--) {
            DraggableCircle figure = figures[i];
            if (figure.isFixed()) {
                canvas.drawCircle(figure.getAimPoint().x, figure.getAimPoint().y, figure.getRadius(), lockingPaint);
                amountOfFixed++;
            } else {
                canvas.drawCircle(figure.getAimPoint().x, figure.getAimPoint().y, figure.getRadius(), spiritPaints[i]);
                canvas.drawCircle(figure.getCurrentPoint().x, figure.getCurrentPoint().y, figure.getRadius(), draggableCirclePaints[i]);
            }
        }

        if (amountOfFixed == 2) {
            EventBus.getDefault().post(AlarmMessage.CANCEL_VOLUME_INCREASE);
        }

        if (amountOfFixed >= figures.length) {
            EventBus.getDefault().postSticky(AlarmMessage.STOP_ALARM);


//            getContext().stopService(new Intent(getContext(), AlarmNotifyingService.class));

        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);

        if (event.getAction() == MotionEvent.ACTION_UP) {
            activeFigure = null;
            invalidate();
            result = true;
        } else {
            final int x = (int) event.getX();
            final int y = (int) event.getY();
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                activeFigure = findFigure(event);
                if (activeFigure != null) {
                    prevX = x;
                    prevY = y;
                    invalidate();
                    result = true;
                }
            } else if (event.getAction() == MotionEvent.ACTION_MOVE && activeFigure != null
                    && viewPortBoundsForEvent.contains(x, y)) {
                figures[activeFigure].offset(x - prevX, y - prevY);

                prevX = x;
                prevY = y;
                invalidate();
                result = true;
            }
        }
        invalidate();
        return result;
    }


    private Integer findFigure(MotionEvent event) {
        for (int i = 0; i < figures.length; i++) {
            if (figures[i].isInFigure(event.getX(), event.getY())) {
                return i;
            }
        }

        return null;
    }
}
