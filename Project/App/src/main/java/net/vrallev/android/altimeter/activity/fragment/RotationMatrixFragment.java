package net.vrallev.android.altimeter.activity.fragment;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.vrallev.android.altimeter.R;
import net.vrallev.android.altimeter.activity.InitializeActivity;

/**
 * @author Ralf Wondratschek
 */
@SuppressWarnings("ConstantConditions")
public class RotationMatrixFragment extends AbstractSensorFragment {

    private float[] mRotationMatrixRotation;
    private float[] mRotationMatrixAcc;
    private float[] mAcceleration;
    private float[] mMagneticField;

    private Sensor mSensorAcceleration;
    private Sensor mSensorMagnet;
    private Sensor mSensorGravity;

    protected TextView mTextViewX2;
    protected TextView mTextViewY2;
    protected TextView mTextViewZ2;

    protected TextView mTextViewGravityX;
    protected TextView mTextViewGravityY;
    protected TextView mTextViewGravityZ;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAcceleration = new float[3];
        mMagneticField = new float[3];

        mRotationMatrixRotation = new float[]{
                1f, 0f, 0f,
                0f, 1f, 0f,
                0f, 0f, 1f};
        mRotationMatrixAcc = new float[]{
                1f, 0f, 0f,
                0f, 1f, 0f,
                0f, 0f, 1f};

        mSensorAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorMagnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorGravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rotation_matrix, container, false);

        mTextViewX = (TextView) view.findViewById(R.id.textView_x);
        mTextViewY = (TextView) view.findViewById(R.id.textView_y);
        mTextViewZ = (TextView) view.findViewById(R.id.textView_z);

        mTextViewX2 = (TextView) view.findViewById(R.id.textView_x_2);
        mTextViewY2 = (TextView) view.findViewById(R.id.textView_y_2);
        mTextViewZ2 = (TextView) view.findViewById(R.id.textView_z_2);

        mTextViewGravityX = (TextView) view.findViewById(R.id.textView_gravity_x);
        mTextViewGravityY = (TextView) view.findViewById(R.id.textView_gravity_y);
        mTextViewGravityZ = (TextView) view.findViewById(R.id.textView_gravity_z);

        view.findViewById(R.id.button_start_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), InitializeActivity.class));
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, mSensorAcceleration, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, mSensorMagnet, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, mSensorGravity, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected int getSensor() {
        return Sensor.TYPE_ROTATION_VECTOR;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        super.onSensorChanged(event);

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ROTATION_VECTOR:
                SensorManager.getRotationMatrixFromVector(mRotationMatrixRotation, event.values);

                double x = Math.atan2(mRotationMatrixRotation[7], mRotationMatrixRotation[8]);
                double y = Math.atan2(mRotationMatrixRotation[6] * -1, Math.sqrt(Math.pow(mRotationMatrixRotation[7], 2) + Math.pow(mRotationMatrixRotation[8], 2)));
                double z = Math.atan2(mRotationMatrixRotation[3], mRotationMatrixRotation[0]);

                publishSensorValues((float) x, (float) y, (float) z, DEFAULT_DIGITS_BEFORE, DEFAULT_DIGITS_AFTER);
                break;

            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, mAcceleration, 0, mAcceleration.length);
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, mMagneticField, 0, mMagneticField.length);
                break;

            case Sensor.TYPE_GRAVITY:
                mTextViewGravityX.setText(avoidJumpingText(event.values[0], DEFAULT_DIGITS_BEFORE, DEFAULT_DIGITS_AFTER));
                mTextViewGravityY.setText(avoidJumpingText(event.values[1], DEFAULT_DIGITS_BEFORE, DEFAULT_DIGITS_AFTER));
                mTextViewGravityZ.setText(avoidJumpingText(event.values[2], DEFAULT_DIGITS_BEFORE, DEFAULT_DIGITS_AFTER));
                break;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER || event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            SensorManager.getRotationMatrix(mRotationMatrixAcc, null, mAcceleration, mMagneticField);

            double x = Math.atan2(mRotationMatrixAcc[7], mRotationMatrixAcc[8]);
            double y = Math.atan2(mRotationMatrixAcc[6] * -1, Math.sqrt(Math.pow(mRotationMatrixAcc[7], 2) + Math.pow(mRotationMatrixAcc[8], 2)));
            double z = Math.atan2(mRotationMatrixAcc[3], mRotationMatrixAcc[0]);

            mTextViewX2.setText(avoidJumpingText((float) x, DEFAULT_DIGITS_BEFORE, DEFAULT_DIGITS_AFTER));
            mTextViewY2.setText(avoidJumpingText((float) y, DEFAULT_DIGITS_BEFORE, DEFAULT_DIGITS_AFTER));
            mTextViewZ2.setText(avoidJumpingText((float) z, DEFAULT_DIGITS_BEFORE, DEFAULT_DIGITS_AFTER));
        }


    }
}
