package net.vrallev.android.altimeter.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import net.vrallev.android.altimeter.activity.InitializeCarPositionActivity;

/**
 * @author Ralf Wondratschek
 */
@SuppressWarnings({"UnusedDeclaration", "ConstantConditions"})
public class GapProgressView extends View {

    private int mWidth;
    private int mHeight;

    private Paint mBadPaint;
    private Paint mGoodPaint;

    private double[] mData;

    public GapProgressView(Context context) {
        super(context);
        constructor(context, null, 0);
    }

    public GapProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        constructor(context, attrs, 0);
    }

    public GapProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        constructor(context, attrs, defStyleAttr);
    }

    protected void constructor(Context context, AttributeSet attrs, int defStyleAttr) {
        mBadPaint = new Paint();
        mBadPaint.setAntiAlias(true);
        mBadPaint.setColor(getResources().getColor(android.R.color.holo_red_light));

        mGoodPaint = new Paint(mBadPaint);
        mGoodPaint.setColor(getResources().getColor(android.R.color.holo_green_light));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(0, 0, mWidth, mHeight, mBadPaint);
        if (mData == null) {
            return;
        }

        for (int i = 0; i < mData.length; i++) {
            double v = mData[i];
            if (v != InitializeCarPositionActivity.INVALID) {
                float x = i / (float) mData.length * mWidth;
                canvas.drawLine(x, 0, x, mHeight, mGoodPaint);
            }
         }
    }

    public double[] getData() {
        return mData;
    }

    public void setData(double[] data) {
        mData = data;
        invalidate();
    }
}
