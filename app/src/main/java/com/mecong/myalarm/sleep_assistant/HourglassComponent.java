package com.mecong.myalarm.sleep_assistant;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.mecong.myalarm.R;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import static java.lang.Math.max;
import static java.lang.Math.min;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class HourglassComponent extends View {
    Color mcolor;
    long max, currentValue;
    boolean changingMode = false;
    Paint filledPaint = new Paint();
    Paint framedPaint = new Paint();
    Paint changingPaint = new Paint();
    List<SleepTimerViewValueListener> listeners = new ArrayList<>(1);
    Rect frame;
    float frameHeightDivMax, maxDivFrameHeight;

    public HourglassComponent(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.HourglassComponent, 0, 0);

        try {
            max = a.getInteger(R.styleable.HourglassComponent_max, 100);
            currentValue = a.getInteger(R.styleable.HourglassComponent_currentValue, 30);
        } finally {
            a.recycle();
        }
        filledPaint.setColor(Color.parseColor("#323030"));

        framedPaint.setColor(filledPaint.getColor());
        framedPaint.setStyle(Paint.Style.STROKE);
        framedPaint.setStrokeWidth(1);

        changingPaint.setColor(Color.GRAY);
        changingPaint.setStrokeWidth(5);

        for (SleepTimerViewValueListener listener : listeners) {
            listener.onValueChanged(currentValue);
        }
    }

    public void addListener(SleepTimerViewValueListener listener) {
        listeners.add(listener);
    }


    public void setCurrentValue(long currentValue) {
        this.currentValue = currentValue;

        invalidate();
        requestLayout();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        frame = bounds();

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

//        canvas.drawRoundRect(frame.left, filled, frame.right, frame.bottom,25f,25f, filledPaint);
        canvas.drawRect(frame.left + 10, filled, frame.right - 10, frame.bottom, filledPaint);
//        canvas.drawRect(frame, framedPaint);

//        if (changingMode) {
////            canvas.drawLine(frame.left, filled, frame.right, filled, changingPaint);
////            canvas.drawCircle(0, filled, 5, changingPaint);
////            canvas.drawCircle(frame.width(), filled, 5, changingPaint);
//        }
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
            long newValue = (long) ((frame.height() - event.getY()) * maxDivFrameHeight);
            newValue = min(newValue, max);
            newValue = max(newValue, 5);

            this.currentValue = newValue;
            for (SleepTimerViewValueListener listener : listeners) {
                listener.onValueChanged(currentValue);
            }
            invalidate();
            requestLayout();
            result = true;
        }
        return result;
    }

    public interface SleepTimerViewValueListener {
        void onValueChanged(long newValue);
    }
}