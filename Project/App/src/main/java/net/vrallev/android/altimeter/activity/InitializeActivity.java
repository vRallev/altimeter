package net.vrallev.android.altimeter.activity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import net.vrallev.android.altimeter.R;
import net.vrallev.android.altimeter.activity.fragment.AbstractSensorFragment;
import net.vrallev.android.base.BaseActivity;
import net.vrallev.android.base.util.AndroidServices;
import net.vrallev.android.base.util.Cat;

/**
 * @author Ralf Wondratschek
 */
public class InitializeActivity extends BaseActivity implements SensorEventListener {

    private static final double INVALID = -100;
    private static final float INVALID_FLOAT = -100f;

    private static final int LOGGING_ACCURACY = 1000;
    private static final int BIGGEST_GAP = 10;

    private static final int DIGITS_BEFORE = 2;
    private static final int DIGITS_AFTER = 3;

    private static final SensorManager SENSOR_MANAGER = AndroidServices.getSensorManager();

    private TextView mTextViewRotationX;
    private TextView mTextViewRotationY;
    private TextView mTextViewRotationZ;
    private TextView mTextViewAccelerationX;
    private TextView mTextViewAccelerationY;
    private TextView mTextViewAccelerationZ;

    private TextView mTextViewSlower;
    private Button mButtonStartRotation;
    private Button mButtonStartAcceleration;

    private Sensor mSensorRotation;
    private Sensor mSensorAcceleration;

    private float[] mRotationMatrix;

    private double[] mLoggedRotationX;
    private int mLoggedEventsCount;
    private int mLastPosition;

    private int mLogCounter;

    private float[] mMaxAccelerations;
    private float[] mMinAccelerations;

    private float[] mAccelerations;
    private int mAccelerationPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialize);

        mTextViewRotationX = (TextView) findViewById(R.id.textView_x);
        mTextViewRotationY = (TextView) findViewById(R.id.textView_y);
        mTextViewRotationZ = (TextView) findViewById(R.id.textView_z);
        mTextViewAccelerationX = (TextView) findViewById(R.id.textView_acceleration_x);
        mTextViewAccelerationY = (TextView) findViewById(R.id.textView_acceleration_y);
        mTextViewAccelerationZ = (TextView) findViewById(R.id.textView_acceleration_z);

        mTextViewSlower = (TextView) findViewById(R.id.textView_slower);
        mButtonStartRotation = (Button) findViewById(R.id.button_start_test);
        mButtonStartAcceleration = (Button) findViewById(R.id.button_start_test_2);

        mButtonStartRotation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setVisibility(View.INVISIBLE);
                initValues();
            }
        });

        mButtonStartAcceleration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setVisibility(View.INVISIBLE);
                initValues();
            }
        });

        mSensorRotation = SENSOR_MANAGER.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mSensorAcceleration = SENSOR_MANAGER.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        mRotationMatrix = new float[]{
                1f, 0f, 0f,
                0f, 1f, 0f,
                0f, 0f, 1f};

        mMaxAccelerations = new float[3];
        mMinAccelerations = new float[3];

        mAccelerations = new float[1000];
    }

    private void initValues() {
        mLoggedRotationX = new double[(int) (Math.PI * 2 * LOGGING_ACCURACY + 1)];
        for (int i = 0; i < mLoggedRotationX.length; i++) {
            mLoggedRotationX[i] = INVALID;
        }
        mLoggedEventsCount = 0;
        mLastPosition = -1;

        for (int i = 0; i < mMaxAccelerations.length; i++) {
            mMaxAccelerations[i] = -100;
            mMinAccelerations[i] = 100;
        }

        for (int i = 0; i < mAccelerations.length; i++) {
            mAccelerations[i] = INVALID_FLOAT;
        }
        mAccelerationPosition = 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        SENSOR_MANAGER.registerListener(this, mSensorRotation, SensorManager.SENSOR_DELAY_FASTEST);
        SENSOR_MANAGER.registerListener(this, mSensorAcceleration, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        SENSOR_MANAGER.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ROTATION_VECTOR:
                onSensorChangedRotation(event);
                break;

            case Sensor.TYPE_LINEAR_ACCELERATION:
                onSensorChangedAcceleration(event);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Cat.d("OnAccuracyChanged");
    }

    public void onSensorChangedRotation(SensorEvent event) {
        SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);

        double x = Math.atan2(mRotationMatrix[7], mRotationMatrix[8]);
        double y = Math.atan2(mRotationMatrix[6] * -1, Math.sqrt(Math.pow(mRotationMatrix[7], 2) + Math.pow(mRotationMatrix[8], 2)));
        double z = Math.atan2(mRotationMatrix[3], mRotationMatrix[0]);

        mTextViewRotationX.setText(AbstractSensorFragment.avoidJumpingText((float) x, DIGITS_BEFORE, DIGITS_AFTER));
        mTextViewRotationY.setText(AbstractSensorFragment.avoidJumpingText((float) y, DIGITS_BEFORE, DIGITS_AFTER));
        mTextViewRotationZ.setText(AbstractSensorFragment.avoidJumpingText((float) z, DIGITS_BEFORE, DIGITS_AFTER));

        if (mButtonStartRotation.getVisibility() == View.VISIBLE) {
            return;
        }

        int pos = (int) ((z + Math.PI) * LOGGING_ACCURACY);

        if (mLoggedRotationX[pos] == INVALID) {
            mLoggedEventsCount++;
            if (mLoggedEventsCount % 100 == 0) {
                Cat.d("Logged events %d %d", mLoggedEventsCount, pos);
            }
        }

        if (Math.abs(mLastPosition - pos) >= BIGGEST_GAP / 2) {
            mTextViewSlower.setVisibility(View.VISIBLE);
        } else {
            mTextViewSlower.setVisibility(View.INVISIBLE);
        }

        mLastPosition = pos;

        if (mLoggedEventsCount > LOGGING_ACCURACY * Math.PI) {
            int biggestGap = getBiggestGap(mLoggedRotationX);

            if (biggestGap < 12) {
                showResultRotation();
            }

            if (mLogCounter++ % 200 == 0) {
                Cat.d("Biggest Gap: %d", biggestGap);
                mLogCounter = 1;
            }
        }

        mLoggedRotationX[pos] = x;
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    public void onSensorChangedAcceleration(SensorEvent event) {
        if (mButtonStartAcceleration.getVisibility() == View.VISIBLE) {
            mTextViewAccelerationX.setText(AbstractSensorFragment.avoidJumpingText(event.values[0], DIGITS_BEFORE, DIGITS_AFTER));
            mTextViewAccelerationY.setText(AbstractSensorFragment.avoidJumpingText(event.values[1], DIGITS_BEFORE, DIGITS_AFTER));
            mTextViewAccelerationZ.setText(AbstractSensorFragment.avoidJumpingText(event.values[2], DIGITS_BEFORE, DIGITS_AFTER));
            return;
        }

//        if (event.values[2] > mMaxAccelerations[2]) {
//            System.arraycopy(event.values, 0, mMaxAccelerations, 0, 3);
//        }
//        if (event.values[2] < mMinAccelerations[2]) {
//            System.arraycopy(event.values, 0, mMinAccelerations, 0, 3);
//        }

//        for (int i = 0; i < event.values.length; i++) {
//            mMaxAccelerations[i] = Math.max(event.values[i], mMaxAccelerations[i]);
//            mMinAccelerations[i] = Math.min(event.values[i], mMinAccelerations[i]);
//        }

//        mTextViewAccelerationX.setText(AbstractSensorFragment.avoidJumpingText(mMaxAccelerations[0], DIGITS_BEFORE, DIGITS_AFTER));
//        mTextViewAccelerationY.setText(AbstractSensorFragment.avoidJumpingText(mMaxAccelerations[1], DIGITS_BEFORE, DIGITS_AFTER));
//        mTextViewAccelerationZ.setText(AbstractSensorFragment.avoidJumpingText(mMaxAccelerations[2], DIGITS_BEFORE, DIGITS_AFTER));

        mTextViewAccelerationX.setText("" + mAccelerationPosition);

        int accX = (int) (event.values[0] * 10);
        int accZ = (int) (event.values[2] * 10);

        if (accZ != 0) {
            float degree = calcDegree(accX, accZ);
            mAccelerations[mAccelerationPosition] = degree;
            mAccelerationPosition++;
        }

        if (mAccelerationPosition == mAccelerations.length) {
            float sum = 0;
            for (int i = 0; i < mAccelerations.length; i++) {
                sum += mAccelerations[i];
            }

            sum /= mAccelerations.length;


            Toast.makeText(this, "Degree " + sum, Toast.LENGTH_LONG).show();

            mButtonStartAcceleration.setVisibility(View.VISIBLE);
        }


//        float min = 100;
//        float max = -100;
//        for (int i = 0; i < mMaxAccelerations.length; i++) {
//            max = Math.max(mMaxAccelerations[i], max);
//        }
//        for (int i = 0; i < mMinAccelerations.length; i++) {
//            min = Math.min(mMinAccelerations[i], min);
//        }
//
//        if (max > 2 && min < -2) {
//            showResultAcceleration();
//        }
    }

    private void showResultRotation() {
        double min = 100;
        double max = -100;

        for (double value : mLoggedRotationX) {
            if (value < min && value != INVALID) {
                min = value;
            } else if (value > max) {
                max = value;
            }
        }

        double ascent = (max - min) / 2;
        Toast.makeText(this, "Ascent " + ascent + "\nAscent " + Math.toDegrees(ascent), Toast.LENGTH_LONG).show();

        mButtonStartRotation.setVisibility(View.VISIBLE);
    }

    private void showResultAcceleration() {
        float degreeMax;
        float degreeMin;

        float mAccX = mMaxAccelerations[0];
        float mAccZ = mMaxAccelerations[2];
        if (mAccX < mAccZ) {
            degreeMax = mAccX / mAccZ * 45;
        } else {
            degreeMax = 45 + mAccZ / mAccX * 45;
        }

        mAccX = mMinAccelerations[0];
        mAccZ = mMinAccelerations[2];
        if (mAccX < mAccZ) {
            degreeMin = mAccX / mAccZ * 45;
        } else {
            degreeMin = 45 + mAccZ / mAccX * 45;
        }

        Toast.makeText(this, "Degree " + degreeMax + "\nDegree " + degreeMin, Toast.LENGTH_LONG).show();

        mButtonStartAcceleration.setVisibility(View.VISIBLE);
    }

    private static float calcDegree(float accX, float accZ) {
        return accX / accZ * 45;
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    private static int getBiggestGap(double[] array) {
        int max = -1;
        int cur = 0;

        for (int i = 0; i < array.length; i++) {
            double v = array[i];

            if (v == INVALID) {
                cur++;
            } else {
                max = Math.max(cur, max);
                cur = 0;
            }
        }

        int pos1 = -1;
        int pos2 = -1;
        for (int i = 0; i < array.length; i++) {
            double v = array[i];
            if (v != INVALID) {
                pos1 = i;
                break;
            }
        }
        if (pos1 == -1) {
            return array.length;
        }
        for (int i = array.length - 1; i >= 0; i--) {
            double v = array[i];
            if (v != INVALID) {
                pos2 = i;
                break;
            }
        }
        pos1 += array.length - pos2 - 1;

        return Math.max(max, pos1);
    }
}