package net.vrallev.android.altimeter.activity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import net.vrallev.android.altimeter.R;
import net.vrallev.android.altimeter.activity.fragment.AbstractSensorFragment;
import net.vrallev.android.base.BaseActivity;
import net.vrallev.android.base.util.AndroidServices;
import net.vrallev.android.base.util.Cat;

/**
 * @author Ralf Wondratschek
 */
public class InitializeActivity extends BaseActivity implements SensorEventListener {

    private static final int DIGITS_BEFORE = 2;
    private static final int DIGITS_AFTER = 3;

    private static final SensorManager SENSOR_MANAGER = AndroidServices.getSensorManager();

    private TextView mTextViewRotationX;
    private TextView mTextViewRotationY;
    private TextView mTextViewRotationZ;

    private TextView mTextViewGravityX;
    private TextView mTextViewGravityY;
    private TextView mTextViewGravityZ;

    private Sensor mSensorRotation;
    private Sensor mSensorGravity;

    private float[] mRotationMatrix;

    private LoggedEvent[] mLoggedEvents;
    private int mLoggedEventsCount;
    private int mStartPosition;
    private int mLastPosition;

    private int[] mDirectionArray;
    private boolean mHasDirection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialize);

        mTextViewRotationX = (TextView) findViewById(R.id.textView_x);
        mTextViewRotationY = (TextView) findViewById(R.id.textView_y);
        mTextViewRotationZ = (TextView) findViewById(R.id.textView_z);

        mTextViewGravityX = (TextView) findViewById(R.id.textView_gravity_x);
        mTextViewGravityY = (TextView) findViewById(R.id.textView_gravity_y);
        mTextViewGravityZ = (TextView) findViewById(R.id.textView_gravity_z);

        mRotationMatrix = new float[]{
                1f, 0f, 0f,
                0f, 1f, 0f,
                0f, 0f, 1f};

        mSensorRotation = SENSOR_MANAGER.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mSensorGravity = SENSOR_MANAGER.getDefaultSensor(Sensor.TYPE_GRAVITY);

        mLoggedEvents = new LoggedEvent[(int) (Math.PI * 2 * 100)];
        mStartPosition = -1;
        mLastPosition = -1;

        mDirectionArray = new int[10];
        for (int i = 0; i < mDirectionArray.length; i++) {
            mDirectionArray[i] = -1;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SENSOR_MANAGER.registerListener(this, mSensorRotation, SensorManager.SENSOR_DELAY_UI);
        SENSOR_MANAGER.registerListener(this, mSensorGravity, SensorManager.SENSOR_DELAY_UI);
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
                SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);

                double x = Math.atan2(mRotationMatrix[7], mRotationMatrix[8]);
                double y = Math.atan2(mRotationMatrix[6] * -1, Math.sqrt(Math.pow(mRotationMatrix[7], 2) + Math.pow(mRotationMatrix[8], 2)));
                double z = Math.atan2(mRotationMatrix[3], mRotationMatrix[0]);

                mTextViewRotationX.setText(AbstractSensorFragment.avoidJumpingText((float) x, DIGITS_BEFORE, DIGITS_AFTER));
                mTextViewRotationY.setText(AbstractSensorFragment.avoidJumpingText((float) y, DIGITS_BEFORE, DIGITS_AFTER));
                mTextViewRotationZ.setText(AbstractSensorFragment.avoidJumpingText((float) z, DIGITS_BEFORE, DIGITS_AFTER));

                int pos = (int) ((z + Math.PI) * 100);
                if (mStartPosition < 0) {
                    mStartPosition = pos;
                }
                if (mLoggedEvents[pos] == null) {
                    mLoggedEventsCount++;
                    Cat.d("Logged events %d", mLoggedEventsCount);
                }
                if (mLoggedEventsCount > 15 && !mHasDirection) {
                    setDirection(pos);
                }

                if (mHasDirection && isRotatingRight() && mLastPosition >= mStartPosition && pos < mStartPosition) {
                    Cat.d("Success");
                } else if (mHasDirection && !isRotatingRight() && mLastPosition <= mStartPosition && pos > mStartPosition) {
                    Cat.d("Success");
                }

                mLastPosition = pos;
                mLoggedEvents[pos] = new LoggedEvent();

                break;

            case Sensor.TYPE_GRAVITY:
                mTextViewGravityX.setText(AbstractSensorFragment.avoidJumpingText(event.values[0], DIGITS_BEFORE, DIGITS_AFTER));
                mTextViewGravityY.setText(AbstractSensorFragment.avoidJumpingText(event.values[1], DIGITS_BEFORE, DIGITS_AFTER));
                mTextViewGravityZ.setText(AbstractSensorFragment.avoidJumpingText(event.values[2], DIGITS_BEFORE, DIGITS_AFTER));
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Cat.d("OnAccuracyChanged");
    }

    private void setDirection(int pos) {
        if (mHasDirection) {
            return;
        }

        for (int i = 0; i < mDirectionArray.length; i++) {
            if (mDirectionArray[i] < 0) {
                mDirectionArray[i] = pos;
                return;
            }
        }

        mHasDirection = true;

        Cat.d("Rotating right %b", isRotatingRight());
    }

    private boolean isRotatingRight() {
        if (!mHasDirection) {
            throw new IllegalStateException();
        }

        return mDirectionArray[0] > mDirectionArray[mDirectionArray.length - 1];
    }

    private static class LoggedEvent {

    }
}
