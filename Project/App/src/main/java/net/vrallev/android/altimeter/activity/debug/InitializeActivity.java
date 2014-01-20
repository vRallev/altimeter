package net.vrallev.android.altimeter.activity.debug;

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

    private static final int LOGGING_ACCURACY = 1000;
    private static final int BIGGEST_GAP = 10;

    private static final int MAX_ACCELERATION_COUNT = 3000;

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

    private double mAccelerationSum;
    private int mAccelerationCount;

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

        mAccelerationSum = 0;
        mAccelerationCount = 0;
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

        mTextViewAccelerationX.setText("" + mAccelerationCount);

        int accX = (int) (event.values[0] * 10);
        int accZ = (int) (event.values[2] * 10);

        if (accZ != 0) {
            float degree = calcDegree(accX, accZ);
            mAccelerationSum += degree;
            mAccelerationCount++;
        }

        if (mAccelerationCount == MAX_ACCELERATION_COUNT) {
            showResultAcceleration();
        }
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
        Toast.makeText(this, "Degree " + (mAccelerationSum / MAX_ACCELERATION_COUNT), Toast.LENGTH_LONG).show();
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