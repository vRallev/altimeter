package net.vrallev.android.altimeter.activity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import net.vrallev.android.altimeter.BuildConfig;
import net.vrallev.android.altimeter.R;
import net.vrallev.android.altimeter.file.AbstractCsvWriter;
import net.vrallev.android.altimeter.location.LocationProvider;
import net.vrallev.android.altimeter.location.LocationUtil;
import net.vrallev.android.altimeter.view.HeightDrawerView;
import net.vrallev.android.base.BaseActivity;
import net.vrallev.android.base.util.AndroidServices;

import java.io.IOException;
import java.util.Locale;

/**
 * @author Ralf Wondratschek
 */
@SuppressWarnings("ConstantConditions")
public class HeightMeasurementActivity extends BaseActivity implements SensorEventListener {

    private static final SensorManager SENSOR_MANAGER = AndroidServices.getSensorManager();
    private static final LocationProvider LOCATION_PROVIDER = LocationProvider.getInstance();

    private Sensor mSensor;

    private TestWriterAll mTestWriter;
    private boolean mLogging;

    private Location mOldLocation;

    private float[] mRotationMatrix;
    private double[] mStoredXRotation;
    private int mStoredRotationIndex;

    private HeightDrawerView mHeightDrawerView;
    private Button mButtonStart;
    private Button mButtonStop;
    private TextView mTextViewHeight;

    private double mHeightSum;
    private double mInitialXRotation; // TODO: remove

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_height_measurement);

        mHeightDrawerView = (HeightDrawerView) findViewById(R.id.heightDrawerView);
        mButtonStart = (Button) findViewById(R.id.button_start);
        mButtonStop = (Button) findViewById(R.id.button_stop);
        mTextViewHeight = (TextView) findViewById(R.id.textView_height);

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
                }
            }
        };

        mButtonStart.setOnClickListener(onClickListener);
        mButtonStop.setOnClickListener(onClickListener);

        LOCATION_PROVIDER.start(this);

        mRotationMatrix = new float[]{
                1f, 0f, 0f,
                0f, 1f, 0f,
                0f, 0f, 1f};

        mStoredXRotation = new double[1024];
        mInitialXRotation = Double.MAX_VALUE;

        mSensor = SENSOR_MANAGER.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

    @Override
    public void onResume() {
        super.onResume();
        mButtonStart.setKeepScreenOn(true);
        SENSOR_MANAGER.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        SENSOR_MANAGER.unregisterListener(this);
        mButtonStop.setKeepScreenOn(false);
        super.onPause();
    }

    @Override
    protected void onStop() {
        setLoggingEnabled(false);
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        LOCATION_PROVIDER.stop();
        super.onBackPressed();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }

    private void startTracking() {
        mButtonStart.setVisibility(View.GONE);
        mButtonStop.setVisibility(View.VISIBLE);

        setLoggingEnabled(!BuildConfig.DEBUG);
    }

    private void stopTracking() {
        mButtonStart.setVisibility(View.VISIBLE);
        mButtonStop.setVisibility(View.GONE);

        setLoggingEnabled(false);

        mStoredRotationIndex = 0;
        mHeightDrawerView.resetHeight();
        mTextViewHeight.setText("");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mButtonStart.getVisibility() == View.VISIBLE) {
            return;
        }

        SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);

        double x = Math.atan2(mRotationMatrix[7], mRotationMatrix[8]);
        double y = Math.atan2(mRotationMatrix[6] * -1, Math.sqrt(Math.pow(mRotationMatrix[7], 2) + Math.pow(mRotationMatrix[8], 2)));
        double z = Math.atan2(mRotationMatrix[3], mRotationMatrix[0]);

        if (mInitialXRotation < (Math.PI * -1) || mInitialXRotation > Math.PI) {
            mInitialXRotation = x;
        }

//        mRotationTextView.setText(getString(R.string.rotation_x, x));
//        mRotationInitialTextView.setText(getString(R.string.rotation_x, (x - mInitialXRotation)));

        mStoredXRotation[mStoredRotationIndex] = x - mInitialXRotation;
        mStoredRotationIndex++;
        checkArraySize();

        Location location = LOCATION_PROVIDER.getLocation();
        double distance = 0;
        double height = 0;

        if (mOldLocation != null && !mOldLocation.equals(location)) {
            distance = LocationUtil.getDistanceInKm(location, mOldLocation);

            // we have distance, now we can calculate the height difference
            final double distanceSection = distance / mStoredRotationIndex;

            for (int i = 0; i < mStoredRotationIndex; i++) {
                height += Math.tan(mStoredXRotation[i]) * distanceSection;
            }

            mHeightSum += height;

            mHeightDrawerView.insertHeight(height);
            mTextViewHeight.setText(getString(R.string.height_sum, mHeightSum * 1000));

            mStoredRotationIndex = 0;
        }

        mOldLocation = location;

        if (mLogging) {
            mTestWriter.addEntry(new TestEvent(System.currentTimeMillis(), location.getLongitude(), location.getLatitude(), distance, x, y, z, height, mHeightSum));
        }
    }

    private void setLoggingEnabled(boolean enabled) {
        if (mLogging == enabled) {
            return;
        }

        mLogging = enabled;
        invalidateOptionsMenu();

        if (enabled) {
//            mTestWriter = mSwitch.isChecked() ? new TestWriterAll(this) : new TestWriterSimple(this);
            mTestWriter = new TestWriterAll(this);
            mTestWriter.startWriting();
        } else {
            mTestWriter.stopWriting();
            mTestWriter = null;
        }

//        mSwitch.setEnabled(!enabled);
    }

    private void checkArraySize() {
        if (mStoredRotationIndex == mStoredXRotation.length) {
            double[] array = new double[mStoredXRotation.length * 2];
            System.arraycopy(mStoredXRotation, 0, array, 0, mStoredXRotation.length);
            mStoredXRotation = array;
        }
    }


    public static class TestEvent {

        public final long mTimeStamp;
        public final double mXRotation;
        public final double mYRotation;
        public final double mZRotation;
        public final double mLongitude;
        public final double mLatitude;
        public final double mDistance;
        public final double mHeightDif;
        public final double mHeightSum;

        public TestEvent(long timeStamp, double longitude, double latitude, double distance, double XRotation, double YRotation, double ZRotation, double heightDif, double heightSum) {
            mTimeStamp = timeStamp;
            mLongitude = longitude;
            mDistance = distance;
            mLatitude = latitude;
            mXRotation = XRotation;
            mYRotation = YRotation;
            mZRotation = ZRotation;
            mHeightDif = heightDif;
            mHeightSum = heightSum;
        }
    }


    public static class TestWriterAll extends AbstractCsvWriter<TestEvent> {

        public TestWriterAll(Context context) {
            super(context, "Test_" + System.currentTimeMillis() + ".csv");
        }

        @Override
        protected void writeEvent(TestEvent event) throws IOException {
            writeLine(String.format(Locale.GERMANY, "%d;%f;%f;%f;%f;%f;%f;%f;%f", event.mTimeStamp, event.mLongitude, event.mLatitude, event.mDistance, event.mXRotation, event.mYRotation, event.mZRotation, event.mHeightDif, event.mHeightSum));
        }

        @Override
        protected String getHeader() {
            return "Timestamp;Longitude;Latitude;Distance;X-Rotation;Y-Rotation;Z-Rotation;Height Dif;Height Sum";
        }
    }

//    public static class TestWriterSimple extends TestWriterAll {
//
//        public TestWriterSimple(Context context) {
//            super(context);
//        }
//
//        @Override
//        protected void writeEvent(TestEvent event) throws IOException {
//            if (event.mDistance != 0) {
//                writeLine(String.format(Locale.GERMANY, "%d;%f;%f;%f", event.mTimeStamp, event.mDistance, event.mHeightDif, event.mHeightSum));
//            }
//        }
//
//        @Override
//        protected String getHeader() {
//            return "Timestamp;Distance;Height Dif;Height Sum";
//        }
//    }
}
