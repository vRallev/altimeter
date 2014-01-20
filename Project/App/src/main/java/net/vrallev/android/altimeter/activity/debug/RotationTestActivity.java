package net.vrallev.android.altimeter.activity.debug;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.TextView;

import net.vrallev.android.altimeter.R;
import net.vrallev.android.altimeter.file.AbstractCsvWriter;
import net.vrallev.android.altimeter.location.LocationProvider;
import net.vrallev.android.altimeter.location.LocationUtil;
import net.vrallev.android.base.BaseActivity;
import net.vrallev.android.base.util.AndroidServices;
import net.vrallev.android.base.util.Cat;

import java.io.IOException;
import java.util.Locale;

/**
 * @author Ralf Wondratschek
 */
@SuppressWarnings("ConstantConditions")
public class RotationTestActivity extends BaseActivity implements SensorEventListener {

    private static SensorManager sensorManager = AndroidServices.getSensorManager();
    private static LocationProvider locationProvider = LocationProvider.getInstance();

    private Sensor mSensor;

    private TestWriterAll mTestWriter;
    private boolean mLogging;

    private Location mOldLocation;

    private float[] mRotationMatrix;
    private double[] mStoredXRotation;
    private int mStoredRotationIndex;

    private CompoundButton mSwitch;
    private TextView mRotationTextView;
    private TextView mRotationInitialTextView;
    private TextView mHeightTextView;
    private TextView mHeightSumTextView;

    private double mHeightSum;
    private double mInitialXRotation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rotation_test);

        mSwitch = (CompoundButton) findViewById(R.id.switch1);
        mRotationTextView = (TextView) findViewById(R.id.textView_rotation);
        mRotationInitialTextView = (TextView) findViewById(R.id.textView_rotation_initial);
        mHeightTextView = (TextView) findViewById(R.id.textView_height);
        mHeightSumTextView = (TextView) findViewById(R.id.textView_height_sum);

        locationProvider.start(this);

        mRotationMatrix = new float[]{
                1f, 0f, 0f,
                0f, 1f, 0f,
                0f, 0f, 1f};

        mStoredXRotation = new double[1024];
        mInitialXRotation = Double.MAX_VALUE;

        mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        locationProvider.stop();
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.log, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_log_start).setVisible(!mLogging);
        menu.findItem(R.id.action_log_stop).setVisible(mLogging);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_log_start:
                setLoggingEnabled(true);
                return true;

            case R.id.action_log_stop:
                setLoggingEnabled(false);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);

        double x = Math.atan2(mRotationMatrix[7], mRotationMatrix[8]);
        double y = Math.atan2(mRotationMatrix[6] * -1, Math.sqrt(Math.pow(mRotationMatrix[7], 2) + Math.pow(mRotationMatrix[8], 2)));
        double z = Math.atan2(mRotationMatrix[3], mRotationMatrix[0]);

        if (mInitialXRotation < (Math.PI * -1) || mInitialXRotation > Math.PI) {
            mInitialXRotation = x;
        }

        mRotationTextView.setText(getString(R.string.rotation_x, x));
        mRotationInitialTextView.setText(getString(R.string.rotation_x, (x - mInitialXRotation)));

        mStoredXRotation[mStoredRotationIndex] = x - mInitialXRotation;
        mStoredRotationIndex++;
        checkArraySize();

        Location location = locationProvider.getLocation();
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

            mHeightTextView.setText(getString(R.string.height, height));
            mHeightSumTextView.setText(getString(R.string.height_sum, mHeightSum));

            mStoredRotationIndex = 0;
        }

        mOldLocation = location;

        if (mLogging) {
            mTestWriter.addEntry(new TestEvent(System.currentTimeMillis(), location.getLongitude(), location.getLatitude(), distance, x, y, z, height, mHeightSum));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Cat.i("onAccuracyChanged");
    }

    private void setLoggingEnabled(boolean enabled) {
        if (mLogging == enabled) {
            return;
        }

        mLogging = enabled;
        invalidateOptionsMenu();

        if (enabled) {
            mTestWriter = mSwitch.isChecked() ? new TestWriterAll(this) : new TestWriterSimple(this);
            mTestWriter.startWriting();
        } else {
            mTestWriter.stopWriting();
            mTestWriter = null;
        }

        mSwitch.setEnabled(!enabled);
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

    public static class TestWriterSimple extends TestWriterAll {

        public TestWriterSimple(Context context) {
            super(context);
        }

        @Override
        protected void writeEvent(TestEvent event) throws IOException {
            if (event.mDistance != 0) {
                writeLine(String.format(Locale.GERMANY, "%d;%f;%f;%f", event.mTimeStamp, event.mDistance, event.mHeightDif, event.mHeightSum));
            }
        }

        @Override
        protected String getHeader() {
            return "Timestamp;Distance;Height Dif;Height Sum";
        }
    }
 }
