package net.vrallev.android.altimeter.activity;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;

import net.vrallev.android.altimeter.R;
import net.vrallev.android.altimeter.view.GapProgressView;
import net.vrallev.android.base.BaseActivity;
import net.vrallev.android.base.util.AndroidServices;
import net.vrallev.android.base.util.Cat;

/**
 * @author Ralf Wondratschek
 */
public class InitializeCarPositionActivity extends BaseActivity implements SensorEventListener {

    private static final SensorManager SENSOR_MANAGER = AndroidServices.getSensorManager();

    public static final int REQUEST_CODE = 847962;

    public static final double INVALID = -100;

    private static final int LOGGING_ACCURACY = 1000;
    private static final int BIGGEST_GAP = 10;

    private Button mButtonStart;
    private Button mButtonReset;
    private Button mButtonFinish;
    private GapProgressView mGapProgressView;
    private TextView mTextViewSlower;

    private Sensor mSensorRotation;

    private float[] mRotationMatrix;

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

        mSensorRotation = SENSOR_MANAGER.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        mLoggedRotationX = new double[(int) (Math.PI * 2 * LOGGING_ACCURACY + 1)];
        mRotationMatrix = new float[]{
                1f, 0f, 0f,
                0f, 1f, 0f,
                0f, 0f, 1f};

        setResult(RESULT_CANCELED, CarPositionResult.DEFAULT.toIntent());
    }

    private void initValues() {
        for (int i = 0; i < mLoggedRotationX.length; i++) {
            mLoggedRotationX[i] = INVALID;
        }
        mLoggedEventsCount = 0;
        mLastPosition = -1;
    }

    @Override
    protected void onResume() {
        super.onResume();
        SENSOR_MANAGER.registerListener(this, mSensorRotation, SensorManager.SENSOR_DELAY_FASTEST);
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
        if (mButtonStart.getVisibility() == View.VISIBLE) {
            return;
        }

        SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);

        double x = Math.atan2(mRotationMatrix[7], mRotationMatrix[8]);
        // double y = Math.atan2(mRotationMatrix[6] * -1, Math.sqrt(Math.pow(mRotationMatrix[7], 2) + Math.pow(mRotationMatrix[8], 2)));
        double z = Math.atan2(mRotationMatrix[3], mRotationMatrix[0]);

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
//        } else if (mLoggedEventsCount > LOGGING_ACCURACY * Math.PI) {
//            int biggestGap = getBiggestGap(mLoggedRotationX);
//
//            if (biggestGap < 12) {
//                calcAscent(z);
//            }
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

        // Test
//        maxZPosition += Math.PI;
//        currentZPosition += Math.PI;
//        double abs = Math.abs(maxZPosition - currentZPosition);
//        double percentage = 1 - (abs / (Math.PI / 2));
//        double initialXRotation = ascent * percentage;
//
//        currentZPosition = (currentZPosition + Math.PI * (1.5));
//        if (currentZPosition >= Math.PI * 2) {
//            currentZPosition -= (Math.PI * 2);
//        }
//
//        abs = Math.abs(maxZPosition - currentZPosition);
//        percentage = 1 - (abs / (Math.PI / 2));
//        double initialYRotation = ascent * percentage;
//
//        CarPositionResult result = new CarPositionResult(ascent, maxZPosition);
//
//        Toast.makeText(this, "" + Math.toDegrees(result.getInitialXRotation(currentZPosition)), Toast.LENGTH_LONG).show();
//        Toast.makeText(this, "" + Math.toDegrees(result.getInitialYRotation(currentZPosition)), Toast.LENGTH_LONG).show();

        setResult(RESULT_OK, new CarPositionResult(ascent, maxZPosition).toIntent());
        stopTracking();
        finish();
    }

    @SuppressWarnings({"ForLoopReplaceableByForEach", "UnusedDeclaration"})
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

    @SuppressWarnings("UnusedDeclaration")
    public static class CarPositionResult {

        public static final CarPositionResult DEFAULT = new CarPositionResult(0, 0);

        private static final String KEY = CarPositionResult.class.getName();

        public static CarPositionResult fromIntent(Intent intent) {
            String json = intent.getStringExtra(KEY);
            if (json == null) {
                return null;
            }
            return new Gson().fromJson(json, CarPositionResult.class);
        }

        private final double mAscent;
        private final double mMaxZPosition;

        public CarPositionResult(double ascent, double maxZPosition) {
            mAscent = ascent;
            mMaxZPosition = maxZPosition;
        }

        public double getAscent() {
            return mAscent;
        }

        public double getMaxZPosition() {
            return mMaxZPosition;
        }

        public double getInitialXRotation(double currentZPosition) {
            currentZPosition += Math.PI;
            double maxZPosition = mMaxZPosition + Math.PI;

            double abs = Math.abs(maxZPosition - currentZPosition);

            double percentage = 1 - (abs / (Math.PI / 2));

            return mAscent * percentage;
        }

        public double getInitialYRotation(double currentZPosition) {
            currentZPosition -= Math.PI / 2;
            if (currentZPosition < -Math.PI) {
                currentZPosition += (Math.PI * 2);
            }

            return getInitialXRotation(currentZPosition);
        }

        public Intent toIntent() {
            Intent data = new Intent();
            data.putExtra(KEY, new Gson().toJson(this));
            return data;
        }
    }
}
