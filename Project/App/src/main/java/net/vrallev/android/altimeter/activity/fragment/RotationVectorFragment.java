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
import net.vrallev.android.altimeter.activity.LogActivity;
import net.vrallev.android.altimeter.activity.RotationTestActivity;
import net.vrallev.android.altimeter.activity.RotationVectorDemo;

/**
 * @author Ralf Wondratschek
 */
@SuppressWarnings("ConstantConditions")
public class RotationVectorFragment extends AbstractSensorFragment {

    private TextView mTextViewExtra1;
    private TextView mTextViewExtra2;

    private float[] mRotationMatrix;

    @Override
    protected int getSensor() {
        return Sensor.TYPE_ROTATION_VECTOR;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRotationMatrix = new float[9];
        mRotationMatrix[0] = 1;
        mRotationMatrix[4] = 1;
        mRotationMatrix[8] = 1;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rotation_vector, container, false);

        mTextViewX = (TextView) view.findViewById(R.id.textView_x);
        mTextViewY = (TextView) view.findViewById(R.id.textView_y);
        mTextViewZ = (TextView) view.findViewById(R.id.textView_z);
        mTextViewExtra1 = (TextView) view.findViewById(R.id.textView_extra1);
        mTextViewExtra2 = (TextView) view.findViewById(R.id.textView_extra2);
        view.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), RotationVectorDemo.class));
            }
        });
        view.findViewById(R.id.button_start_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), RotationTestActivity.class));
            }
        });
        view.findViewById(R.id.button_log_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), LogActivity.class));
            }
        });

        return view;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        super.onSensorChanged(event);

        SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);

        double x = Math.atan2(mRotationMatrix[7], mRotationMatrix[8]);
        double y = Math.atan2(mRotationMatrix[6] * -1, Math.sqrt(Math.pow(mRotationMatrix[7], 2) + Math.pow(mRotationMatrix[8], 2)));
        double z = Math.atan2(mRotationMatrix[3], mRotationMatrix[0]);

        publishSensorValues((float) x, (float) y, (float) z, DEFAULT_DIGITS_BEFORE, DEFAULT_DIGITS_AFTER);
//        mTextViewX.setText(avoidJumpingText((float) x, DEFAULT_DIGITS_BEFORE, DEFAULT_DIGITS_AFTER));
//        mTextViewY.setText(avoidJumpingText((float) y, DEFAULT_DIGITS_BEFORE, DEFAULT_DIGITS_AFTER));
//        mTextViewZ.setText(avoidJumpingText((float) z, DEFAULT_DIGITS_BEFORE, DEFAULT_DIGITS_AFTER));

        mTextViewExtra1.setText(avoidJumpingText(event.values[0], DEFAULT_DIGITS_BEFORE, DEFAULT_DIGITS_AFTER));
        mTextViewExtra2.setText(avoidJumpingText(event.values[1], DEFAULT_DIGITS_BEFORE, DEFAULT_DIGITS_AFTER));

//        publishSensorValues(event.values[0], event.values[1], event.values[2], DEFAULT_DIGITS_BEFORE, DEFAULT_DIGITS_AFTER);
//
//        mTextViewExtra1.setText(avoidJumpingText(event.values[3], DEFAULT_DIGITS_BEFORE, DEFAULT_DIGITS_AFTER));
//        mTextViewExtra2.setText(avoidJumpingText(event.values[4], DEFAULT_DIGITS_BEFORE, DEFAULT_DIGITS_AFTER));
    }
}
