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

import com.google.gson.Gson;

import net.vrallev.android.altimeter.R;
import net.vrallev.android.base.BaseActivity;
import net.vrallev.android.base.util.AndroidServices;

import java.util.Arrays;

/**
 * @author Ralf Wondratschek
 */
public class InitializeDevicePositionActivity extends BaseActivity implements SensorEventListener {

    public static final int REQUEST_CODE = 8471336;

    private static final SensorManager SENSOR_MANAGER = AndroidServices.getSensorManager();

    private static final int MAX_ACCELERATION_COUNT = 3000;

    private Button mButtonStart;
    private Button mButtonReset;
    private Button mButtonFinish;
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
        mButtonReset = (Button) findViewById(R.id.button_reset);
        mButtonFinish = (Button) findViewById(R.id.button_finish);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.button_start:
                        startTracking();
                        break;

                    case R.id.button_reset:
                        stopTracking();
                        break;

                    case R.id.button_finish:
                        calcDegreeResult();
                        break;
                }
            }
        };

        mButtonStart.setOnClickListener(onClickListener);
        mButtonReset.setOnClickListener(onClickListener);
        mButtonFinish.setOnClickListener(onClickListener);

        mSensorAcceleration = SENSOR_MANAGER.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        mMaxAccelerations = new float[3];
        mMinAccelerations = new float[3];

        setResult(RESULT_CANCELED, DevicePositionResult.DEFAULT.toIntent());
    }

    private void initValues() {
        Arrays.fill(mMaxAccelerations, -100);
        Arrays.fill(mMinAccelerations, 100);

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
        mButtonReset.setVisibility(View.VISIBLE);
        mButtonFinish.setVisibility(View.VISIBLE);
    }

    private void stopTracking() {
        mProgressBar.setProgress(0);
        mButtonStart.setVisibility(View.VISIBLE);
        mButtonReset.setVisibility(View.GONE);
        mButtonFinish.setVisibility(View.GONE);
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

        setResult(RESULT_OK, new DevicePositionResult(degree).toIntent());
        stopTracking();
        finish();
    }

    private static float calcDegree(float accX, float accZ) {
        return accX / accZ * 45;
    }

    public static class DevicePositionResult {

        public static final DevicePositionResult DEFAULT = new DevicePositionResult(0);

        private static final String KEY = DevicePositionResult.class.getName();

        public static DevicePositionResult fromIntent(Intent intent) {
            String json = intent.getStringExtra(KEY);
            if (json == null) {
                return null;
            }
            return new Gson().fromJson(json, DevicePositionResult.class);
        }

        private final double mDegree;

        public DevicePositionResult(double degree) {
            mDegree = degree;
        }

        public double getDegree() {
            return mDegree;
        }

        public Intent toIntent() {
            Intent intent = new Intent();
            intent.putExtra(KEY, new Gson().toJson(this));
            return intent;
        }
    }
}
