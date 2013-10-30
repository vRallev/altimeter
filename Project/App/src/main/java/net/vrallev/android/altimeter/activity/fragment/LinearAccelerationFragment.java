package net.vrallev.android.altimeter.activity.fragment;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

/**
 * @author Ralf Wondratschek
 */
public class LinearAccelerationFragment extends AbstractSensorFragment {
    @Override
    protected int getSensor() {
        return Sensor.TYPE_LINEAR_ACCELERATION;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        publishSensorValues(event.values[0], event.values[1], event.values[2], DEFAULT_DIGITS_BEFORE, DEFAULT_DIGITS_AFTER);
    }
}