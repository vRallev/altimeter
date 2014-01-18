package net.vrallev.android.altimeter.activity;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import net.vrallev.android.altimeter.R;
import net.vrallev.android.base.BaseActivity;
import net.vrallev.android.base.util.AndroidServices;

/**
 * @author Ralf Wondratschek
 */
public class InitializeOrientationActivity extends BaseActivity implements SensorEventListener {

    private static final SensorManager SENSOR_MANAGER = AndroidServices.getSensorManager();

    private static final int MAX_ACCELERATION_COUNT = 3000;

    private Button mButtonStart;
    private Button mButtonStop;
    private Button mButtonContinue;
    private ProgressBar mProgressBar;

    private Sensor mSensorAcceleration;

    private float[] mMaxAccelerations;
    private float[] mMinAccelerations;

    private double mAccelerationSum;
    private int mAccelerationCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialize_orientation);

        mButtonStart = (Button) findViewById(R.id.button_start);
        mButtonStop = (Button) findViewById(R.id.button_stop);
        mButtonContinue = (Button) findViewById(R.id.button_continue);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.button_start:
                        startTracking();
                        break;

                    case R.id.button_stop:
                        stopTracking();
                        break;

                    case R.id.button_continue:
                        calcDegreeResult();
                        break;
                }
            }
        };

        mButtonStart.setOnClickListener(onClickListener);
        mButtonStop.setOnClickListener(onClickListener);
        mButtonContinue.setOnClickListener(onClickListener);

        mSensorAcceleration = SENSOR_MANAGER.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        mMaxAccelerations = new float[3];
        mMinAccelerations = new float[3];
    }

    private void initValues() {
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
        SENSOR_MANAGER.registerListener(this, mSensorAcceleration, SensorManager.SENSOR_DELAY_FASTEST);
        mButtonStart.setKeepScreenOn(true);
    }

    @Override
    protected void onPause() {
        mButtonStart.setKeepScreenOn(false);
        SENSOR_MANAGER.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }

    private void startTracking() {
        initValues();
        mProgressBar.setMax(MAX_ACCELERATION_COUNT);
        mProgressBar.setProgress(0);
        mButtonStart.setVisibility(View.GONE);
        mButtonStop.setVisibility(View.VISIBLE);
        mButtonContinue.setVisibility(View.VISIBLE);
    }

    private void stopTracking() {
        mProgressBar.setProgress(0);
        mButtonStart.setVisibility(View.VISIBLE);
        mButtonStop.setVisibility(View.GONE);
        mButtonContinue.setVisibility(View.GONE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mButtonStart.getVisibility() == View.VISIBLE) {
            return;
        }

        int accX = (int) (event.values[0] * 10);
        int accZ = (int) (event.values[2] * 10);

        if (accZ != 0) {
            float degree = calcDegree(accX, accZ);
            mAccelerationSum += degree;
            mAccelerationCount++;

            mProgressBar.setProgress(mAccelerationCount);
        }

        if (mAccelerationCount == MAX_ACCELERATION_COUNT) {
            calcDegreeResult();
        }
    }

    private void calcDegreeResult() {
        double degree = mAccelerationSum / mAccelerationCount;

        Toast.makeText(this, "Degree " + degree, Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, HeightMeasurementActivity.class));

        stopTracking();
    }

    private static float calcDegree(float accX, float accZ) {
        return accX / accZ * 45;
    }
}
