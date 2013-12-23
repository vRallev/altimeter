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

    private static final int LOGGING_ACCURACY = 1000;

    private static final int DIGITS_BEFORE = 2;
    private static final int DIGITS_AFTER = 3;

    private static final SensorManager SENSOR_MANAGER = AndroidServices.getSensorManager();

    private TextView mTextViewRotationX;
    private TextView mTextViewRotationY;
    private TextView mTextViewRotationZ;

    private TextView mTextViewSlower;
    private Button mButtonStart;

    private Sensor mSensorRotation;

    private float[] mRotationMatrix;

    private double[] mLoggedRotationX;
    private int mLoggedEventsCount;
    private int mStartPosition;
    private int mLastPosition;

    private int[] mDirectionArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialize);

        mTextViewRotationX = (TextView) findViewById(R.id.textView_x);
        mTextViewRotationY = (TextView) findViewById(R.id.textView_y);
        mTextViewRotationZ = (TextView) findViewById(R.id.textView_z);

        mTextViewSlower = (TextView) findViewById(R.id.textView_slower);
        mButtonStart = (Button) findViewById(R.id.button_start_test);

        mButtonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setVisibility(View.GONE);
                initValues();
            }
        });

        mSensorRotation = SENSOR_MANAGER.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        mRotationMatrix = new float[]{
                1f, 0f, 0f,
                0f, 1f, 0f,
                0f, 0f, 1f};
    }

    private void initValues() {
        mLoggedRotationX = new double[(int) (Math.PI * 2 * LOGGING_ACCURACY + 1)];
        for (int i = 0; i < mLoggedRotationX.length; i++) {
            mLoggedRotationX[i] = INVALID;
        }
        mLoggedEventsCount = 0;
        mStartPosition = -1;
        mLastPosition = -1;

        mDirectionArray = new int[2];
        for (int i = 0; i < mDirectionArray.length; i++) {
            mDirectionArray[i] = -1;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SENSOR_MANAGER.registerListener(this, mSensorRotation, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        SENSOR_MANAGER.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);

        double x = Math.atan2(mRotationMatrix[7], mRotationMatrix[8]);
        double y = Math.atan2(mRotationMatrix[6] * -1, Math.sqrt(Math.pow(mRotationMatrix[7], 2) + Math.pow(mRotationMatrix[8], 2)));
        double z = Math.atan2(mRotationMatrix[3], mRotationMatrix[0]);

        mTextViewRotationX.setText(AbstractSensorFragment.avoidJumpingText((float) x, DIGITS_BEFORE, DIGITS_AFTER));
        mTextViewRotationY.setText(AbstractSensorFragment.avoidJumpingText((float) y, DIGITS_BEFORE, DIGITS_AFTER));
        mTextViewRotationZ.setText(AbstractSensorFragment.avoidJumpingText((float) z, DIGITS_BEFORE, DIGITS_AFTER));

        if (mButtonStart.getVisibility() == View.VISIBLE) {
            return;
        }

        int pos = (int) ((z + Math.PI) * LOGGING_ACCURACY);

        if (mStartPosition < 0) {
            mStartPosition = pos;
        }

        if (mLoggedRotationX[pos] == INVALID) {
            mLoggedEventsCount++;
            Cat.d("Logged events %d %d", mLoggedEventsCount, pos);
        }

        if (mLoggedEventsCount == 15 && mDirectionArray[0] < 0) {
            mDirectionArray[0] = pos;
        } else if (mLoggedEventsCount == 30 && mDirectionArray[1] < 0) {
            mDirectionArray[1] = pos;
        }

        if (hasDirection() && isRotatingRight() && mLastPosition >= mStartPosition && pos < mStartPosition) {
            showResult();
        } else if (hasDirection() && !isRotatingRight() && mLastPosition <= mStartPosition && pos > mStartPosition) {
            showResult();
        }

        if (Math.abs(mLastPosition - pos) > 2) {
            mTextViewSlower.setVisibility(View.VISIBLE);
        } else {
            mTextViewSlower.setVisibility(View.INVISIBLE);
        }

        mLastPosition = pos;
        mLoggedRotationX[pos] = x;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Cat.d("OnAccuracyChanged");
    }

    private boolean isRotatingRight() {
        if (!hasDirection()) {
            throw new IllegalStateException();
        }

        return mDirectionArray[0] > mDirectionArray[mDirectionArray.length - 1];
    }

    private boolean hasDirection() {
        return mDirectionArray[0] >= 0 && mDirectionArray[1] >= 0;
    }

    private void showResult() {
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

        mButtonStart.setVisibility(View.VISIBLE);
    }
}
