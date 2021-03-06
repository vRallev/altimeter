package net.vrallev.android.altimeter.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.Arrays;

/**
 * @author Ralf Wondratschek
 */
@SuppressWarnings({"ConstantConditions", "ForLoopReplaceableByForEach", "UnusedDeclaration"})
public class HeightDrawerView extends View {

    private static final int MAX_VALUES = 100;

    private double[] mHeightValues;
    private double[] mDistanceValues;

    private int mWidth;
    private int mHeight;

    private Paint mPaintColor;
    private Paint mPaintWhite;

    private Path mPath;

    public HeightDrawerView(Context context) {
        super(context);
        constructor(context, null, 0);
    }

    public HeightDrawerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        constructor(context, attrs, 0);
    }

    public HeightDrawerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        constructor(context, attrs, defStyleAttr);
    }

    private void constructor(Context context, AttributeSet attrs, int defStyleAttr) {
        mHeightValues = new double[MAX_VALUES];
        mDistanceValues = new double[MAX_VALUES];

        mPaintColor = new Paint();
        mPaintColor.setAntiAlias(true);
        mPaintColor.setColor(getResources().getColor(android.R.color.holo_blue_light));
        mPaintColor.setStrokeWidth(15);
        mPaintColor.setDither(true);
        mPaintColor.setStyle(Paint.Style.STROKE);
        mPaintColor.setStrokeJoin(Paint.Join.ROUND);
        mPaintColor.setStrokeCap(Paint.Cap.ROUND);
        mPaintColor.setPathEffect(new CornerPathEffect(50));

        mPaintWhite = new Paint();
        mPaintWhite.setAntiAlias(true);
        mPaintWhite.setColor(Color.WHITE);
        mPaintWhite.setStrokeWidth(1);

        mPath = new Path();

        resetHeight();
//        if (BuildConfig.DEBUG) {
//            mHeightValues[0] = -10;
//            mHeightValues[2] = 10;
//            mHeightValues[3] = 10;
//            mHeightValues[4] = 10;
//            mHeightValues[8] = -10;
//            mHeightValues[9] = -10;
//            mDistanceValues[0] = 5;
//            mDistanceValues[2] = 10;
//            mDistanceValues[3] = 25;
//            mDistanceValues[4] = 5;
//            mDistanceValues[8] = 30;
//            mDistanceValues[9] = 15;
//        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawLine(1, 0, 1, mHeight - 1, mPaintWhite);
        canvas.drawLine(1, mHeight - 1, mWidth, mHeight - 1, mPaintWhite);

        final int offset = 20;

        double heightDif = getHeightDif(mHeightValues);
        if (heightDif == 0) {
            canvas.drawLine(offset, mWidth / 2, mWidth - offset, mWidth / 2, mPaintColor);
            return;
        }

        final int drawWidth = mWidth - 2 * offset;
        final int drawHeight = mHeight - 2 * offset;
        final double distanceMultiplier = drawWidth / (sum(mDistanceValues) - mDistanceValues[0]);

//        final float step = drawWidth / (float) (mHeightValues.length - 1);

        final double minHeight = getMin(mHeightValues);

        float startX = offset;

        mPath.moveTo(startX, getYCoordinate(mHeightValues, 0, minHeight, drawHeight, heightDif, offset));

        for (int i = 1; i < mHeightValues.length; i++) {
            startX += (mDistanceValues[i] * distanceMultiplier);
            mPath.lineTo(startX, getYCoordinate(mHeightValues, i, minHeight, drawHeight, heightDif, offset));
        }

        canvas.drawPath(mPath, mPaintColor);
        mPath.reset();
    }

    public void insertHeight(double height, double distance) {
        shiftLeft(mHeightValues);
        mHeightValues[mHeightValues.length - 1] = height;

        shiftLeft(mDistanceValues);
        mDistanceValues[mDistanceValues.length - 1] = distance;

        invalidate();
    }

    public void resetHeight() {
        Arrays.fill(mHeightValues, 0);
        Arrays.fill(mDistanceValues, 0.001);
        invalidate();
    }

    private static float getYCoordinate(double[] array, int index, double minHeight, int drawHeight, double heightDif, int offset) {
        double height = array[index] - minHeight;
        return drawHeight - ((float) (height / heightDif * drawHeight)) + offset;
    }

    private static double getHeightDif(double[] array) {
        double max = -Double.MAX_VALUE;
        double min = Double.MAX_VALUE;

        for (int i = 0; i < array.length; i++) {
            max = Math.max(array[i], max);
            min = Math.min(array[i], min);
        }

        return max - min;
    }

    private static double getMin(double[] array) {
        double min = Double.MAX_VALUE;
        for (int i = 0; i < array.length; i++) {
            min = Math.min(array[i], min);
        }
        return min;
    }

    private static double getMax(double[] array) {
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < array.length; i++) {
            max = Math.max(array[i], max);
        }
        return max;
    }

    private static void shiftLeft(double[] array) {
        System.arraycopy(array, 1, array, 0, array.length - 1);
    }

    private static double sum(double[] array) {
        double sum = 0;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        return sum;
    }
}
