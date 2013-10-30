package net.vrallev.android.altimeter.activity.fragment;

import android.app.Fragment;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.vrallev.android.altimeter.R;
import net.vrallev.android.base.util.AndroidServices;
import net.vrallev.android.base.util.Cat;

/**
 * @author Ralf Wondratschek
 */
@SuppressWarnings("ConstantConditions")
public abstract class AbstractSensorFragment extends Fragment implements SensorEventListener {

    protected static final int DEFAULT_DIGITS_BEFORE = 2;
    protected static final int DEFAULT_DIGITS_AFTER = 2;

    protected static SensorManager sensorManager = AndroidServices.getSensorManager();

    protected Sensor mSensor;

    protected TextView mTextViewX;
    protected TextView mTextViewY;
    protected TextView mTextViewZ;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSensor = sensorManager.getDefaultSensor(getSensor());
    }

    protected abstract int getSensor();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sensor, container, false);

        mTextViewX = (TextView) view.findViewById(R.id.textView_x);
        mTextViewY = (TextView) view.findViewById(R.id.textView_y);
        mTextViewZ = (TextView) view.findViewById(R.id.textView_z);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Cat.i("onAccuracyChanged");
    }

    protected void publishSensorValues(float x, float y, float z, int digitsBeforDecimalPoint, int digitsAfterDecimalPoint) {
        mTextViewX.setText(avoidJumpingText(x, digitsBeforDecimalPoint, digitsAfterDecimalPoint));
        mTextViewY.setText(avoidJumpingText(y, digitsBeforDecimalPoint, digitsAfterDecimalPoint));
        mTextViewZ.setText(avoidJumpingText(z, digitsBeforDecimalPoint, digitsAfterDecimalPoint));
    }

    protected static float round(float value, int digitsAfterDecimalPoint) {
        digitsAfterDecimalPoint = (int) Math.pow(10, digitsAfterDecimalPoint);
        int v = (int) (value * digitsAfterDecimalPoint);
        return v / (float) digitsAfterDecimalPoint;
    }

    private static StringBuilder stringBuilderCache = new StringBuilder();

    protected static String avoidJumpingText(float value, int digitsBeforDecimalPoint, int digitsAfterDecimalPoint) {
        value = round(value, digitsAfterDecimalPoint);

        stringBuilderCache.delete(0, stringBuilderCache.length());

        if (value < 0) {
            stringBuilderCache.append('-');
            value *= -1;
        } else {
            stringBuilderCache.append(' ');
        }

        String valueString = String.valueOf(value);
        int index = valueString.indexOf('.');
        while (index < digitsBeforDecimalPoint) {
            stringBuilderCache.append(' ');
            index++;
        }

        stringBuilderCache.append(value);
        int valueNumbersAfterComma = valueString.length() - valueString.indexOf('.') - 1;
        while (valueNumbersAfterComma < digitsAfterDecimalPoint) {
            valueNumbersAfterComma++;
            stringBuilderCache.append('0');
        }

        return stringBuilderCache.toString();
    }
}
