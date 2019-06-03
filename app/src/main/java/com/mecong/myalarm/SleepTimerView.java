package com.mecong.myalarm;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class SleepTimerView extends View {
    int max, currentValue;
    boolean changingMode = false;
    Paint filledPaint = new Paint();
    Paint framedPaint = new Paint();
    Paint changingPaint = new Paint();
    List<SleepTimerViewValueListener> listeners = new ArrayList<>(1);
    Rect frame;
    float frameHeightDivMax, maxDivFrameHeight;

    public SleepTimerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.SleepTimerView, 0, 0);

        try {
            max = a.getInteger(R.styleable.SleepTimerView_max, 100);
            currentValue = a.getInteger(R.styleable.SleepTimerView_currentValue, 30);
        } finally {
            a.recycle();
        }

        filledPaint.setColor(Color.LTGRAY);
        framedPaint.setColor(filledPaint.getColor());
        framedPaint.setStyle(Paint.Style.STROKE);
        framedPaint.setStrokeWidth(3);

        changingPaint.setColor(Color.GRAY);
        changingPaint.setStrokeWidth(5);
    }

    public void addListener(SleepTimerViewValueListener listener) {
        listeners.add(listener);
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
        invalidate();
        requestLayout();
    }

    public int getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(int currentValue) {
        this.currentValue = currentValue;
        invalidate();
        requestLayout();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        frame = bounds();
        frame.inset(1, 1);

        frameHeightDivMax = (float) frame.height() / max;
        maxDivFrameHeight = max / (float) frame.height();
    }

    private Rect bounds() {
        return new Rect(getPaddingLeft(), getPaddingTop(),
                getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float filled = frame.height() - frameHeightDivMax * currentValue;

        canvas.drawRect(frame.left, filled, frame.right, frame.bottom, filledPaint);
        canvas.drawRect(frame, framedPaint);

        if (changingMode) {
            canvas.drawLine(frame.left, filled, frame.right, filled, changingPaint);
            canvas.drawCircle(frame.width() * 0.25f, filled, 17, changingPaint);
            canvas.drawCircle(frame.width() * 0.5f, filled, 17, changingPaint);
            canvas.drawCircle(frame.width() * 0.75f, filled, 17, changingPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.performClick();
        boolean result = false;
        if (event.getAction() == MotionEvent.ACTION_UP) {
            changingMode = false;
            invalidate();
            result = true;
        } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
            changingMode = true;
            invalidate();
            result = true;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            currentValue = (int) ((frame.height() - event.getY()) * maxDivFrameHeight);
            invalidate();
            for (SleepTimerViewValueListener listener : listeners) {
                listener.onValueChanged(currentValue);
            }
            result = true;
        }
        return result;
    }

    public interface SleepTimerViewValueListener {
        void onValueChanged(int newValue);
    }
}