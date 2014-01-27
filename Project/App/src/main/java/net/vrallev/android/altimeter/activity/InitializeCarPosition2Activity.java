package net.vrallev.android.altimeter.activity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import net.vrallev.android.altimeter.R;
import net.vrallev.android.altimeter.view.GapProgressView;
import net.vrallev.android.base.BaseActivity;
import net.vrallev.android.base.util.AndroidServices;
import net.vrallev.android.base.util.Cat;

import java.util.Arrays;

/**
 * @author Ralf Wondratschek
 */
public class InitializeCarPosition2Activity extends BaseActivity implements SensorEventListener {

    private static final SensorManager SENSOR_MANAGER = AndroidServices.getSensorManager();

    public static final int REQUEST_CODE = 54712;

    public static final double INVALID = -100;

    private static final int LOGGING_ACCURACY = 1000;
    private static final int BIGGEST_GAP = 10;

    private Button mButtonStart;
    private Button mButtonReset;
    private Button mButtonFinish;
    private GapProgressView mGapProgressView;
    private TextView mTextViewSlower;

    private Sensor mSensorAcceleration;
    private Sensor mSensorMagnet;

    private float[] mRotationMatrixAcc;
    private float[] mAcceleration;
    private float[] mMagneticField;

    private double[] mLoggedRotationX;
    private int mLoggedEventsCount;
    private int mLastPosition;

    private boolean mFinishTracking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialize_bottom_plane);

        mButtonStart = (Button) findViewById(R.id.button_start);
        mButtonReset = (Button) findViewById(R.id.button_reset);
        mButtonFinish = (Button) findViewById(R.id.button_finish);
        mGapProgressView = (GapProgressView) findViewById(R.id.gap_progress_view);
        mTextViewSlower = (TextView) findViewById(R.id.textView_slower);

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
                        mFinishTracking = true;
                        break;
                }
            }
        };

        mButtonStart.setOnClickListener(onClickListener);
        mButtonReset.setOnClickListener(onClickListener);
        mButtonFinish.setOnClickListener(onClickListener);

        mSensorAcceleration = SENSOR_MANAGER.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorMagnet = SENSOR_MANAGER.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mLoggedRotationX = new double[(int) (Math.PI * 2 * LOGGING_ACCURACY + 1)];
        mRotationMatrixAcc = new float[]{
                1f, 0f, 0f,
                0f, 1f, 0f,
                0f, 0f, 1f};

        mAcceleration = new float[3];
        mMagneticField = new float[3];

        setResult(RESULT_CANCELED, InitializeCarPositionActivity.CarPositionResult.DEFAULT.toIntent());
    }

    private void initValues() {
        Arrays.fill(mLoggedRotationX, INVALID);
        mLoggedEventsCount = 0;
        mLastPosition = -1;
    }

    @Override
    protected void onResume() {
        super.onResume();
        SENSOR_MANAGER.registerListener(this, mSensorAcceleration, SensorManager.SENSOR_DELAY_FASTEST);
        SENSOR_MANAGER.registerListener(this, mSensorMagnet, SensorManager.SENSOR_DELAY_FASTEST);
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
        mGapProgressView.setData(mLoggedRotationX);
        mButtonStart.setVisibility(View.GONE);
        mButtonReset.setVisibility(View.VISIBLE);
        mButtonFinish.setVisibility(View.VISIBLE);
    }

    private void stopTracking() {
        mGapProgressView.setData(null);
        mButtonStart.setVisibility(View.VISIBLE);
        mButtonReset.setVisibility(View.GONE);
        mButtonFinish.setVisibility(View.GONE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, mAcceleration, 0, mAcceleration.length);
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, mMagneticField, 0, mMagneticField.length);
                break;
        }

        if (mButtonStart.getVisibility() == View.VISIBLE) {
            return;
        }

        SensorManager.getRotationMatrix(mRotationMatrixAcc, null, mAcceleration, mMagneticField);

        double x = Math.atan2(mRotationMatrixAcc[7], mRotationMatrixAcc[8]);
        // double y = Math.atan2(mRotationMatrixAcc[6] * -1, Math.sqrt(Math.pow(mRotationMatrixAcc[7], 2) + Math.pow(mRotationMatrixAcc[8], 2)));
        double z = Math.atan2(mRotationMatrixAcc[3], mRotationMatrixAcc[0]);

        int pos = (int) ((z + Math.PI) * LOGGING_ACCURACY);

        if (mLoggedRotationX[pos] == INVALID) {
            mLoggedEventsCount++;
            mGapProgressView.invalidate();

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
        mLoggedRotationX[pos] = x;

        if (mFinishTracking) {
            calcAscent();
        }
    }

    private void calcAscent() {
        double min = 100;
        double max = -100;

        int maxPosition = 0;

        for (int i = 0; i < mLoggedRotationX.length; i++) {

            double value = mLoggedRotationX[i];
            if (value < min && value != INVALID) {
                min = value;
            } else if (value > max) {
                max = value;
                maxPosition = i;
            }
        }

        double maxZPosition = maxPosition / (double) LOGGING_ACCURACY - Math.PI;
        double ascent = (max - min) / 2;

        setResult(RESULT_OK, new InitializeCarPositionActivity.CarPositionResult(ascent, maxZPosition).toIntent());
        stopTracking();
        finish();
    }
}
