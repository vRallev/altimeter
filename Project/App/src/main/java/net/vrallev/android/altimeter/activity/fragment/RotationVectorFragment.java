package net.vrallev.android.altimeter.activity.fragment;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.vrallev.android.altimeter.R;
import net.vrallev.android.altimeter.activity.RotationVectorDemo;

/**
 * @author Ralf Wondratschek
 */
@SuppressWarnings("ConstantConditions")
public class RotationVectorFragment extends AbstractSensorFragment {

    private TextView mTextViewExtra1;
    private TextView mTextViewExtra2;

    @Override
    protected int getSensor() {
        return Sensor.TYPE_ROTATION_VECTOR;
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

        return view;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        super.onSensorChanged(event);

        publishSensorValues(event.values[0], event.values[1], event.values[2], DEFAULT_DIGITS_BEFORE, DEFAULT_DIGITS_AFTER);

        mTextViewExtra1.setText(avoidJumpingText(event.values[3], DEFAULT_DIGITS_BEFORE, DEFAULT_DIGITS_AFTER));
        mTextViewExtra2.setText(avoidJumpingText(event.values[4], DEFAULT_DIGITS_BEFORE, DEFAULT_DIGITS_AFTER));
    }
}
