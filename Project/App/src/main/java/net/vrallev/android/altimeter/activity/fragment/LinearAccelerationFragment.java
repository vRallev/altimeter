package net.vrallev.android.altimeter.activity.fragment;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

/**
 * @author Ralf Wondratschek
 */
public class LinearAccelerationFragment extends AbstractSensorFragment {

    private float[] mSums = new float[3];
    private float[] mLastValues = new float[3];
    private long mTimestamp = System.currentTimeMillis();

    @Override
    protected int getSensor() {
        return Sensor.TYPE_LINEAR_ACCELERATION;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        super.onSensorChanged(event);

        for (int i = 0; i < mSums.length; i++) {
            mSums[i] += event.values[i];
        }

        long time = System.currentTimeMillis();

        for (int i = 0; i < mLastValues.length; i++) {
            mLastValues[i] = event.values[i] * (time - mTimestamp) + mLastValues[i];
        }

        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.

        publishSensorValues(event.values[0], event.values[1], event.values[2], DEFAULT_DIGITS_BEFORE, DEFAULT_DIGITS_AFTER);
//        publishSensorValues(mSums[0], mSums[1], mSums[2], DEFAULT_DIGITS_BEFORE, DEFAULT_DIGITS_AFTER);
//        publishSensorValues(mLastValues[0], mLastValues[1], mLastValues[2], DEFAULT_DIGITS_BEFORE, DEFAULT_DIGITS_AFTER);

        mTimestamp = System.currentTimeMillis();
    }
}
