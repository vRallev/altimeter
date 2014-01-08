package net.vrallev.android.altimeter.activity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import net.vrallev.android.altimeter.R;
import net.vrallev.android.altimeter.file.AbstractCsvWriter;
import net.vrallev.android.altimeter.location.LocationProvider;
import net.vrallev.android.base.BaseActivity;
import net.vrallev.android.base.util.AndroidServices;
import net.vrallev.android.base.util.Cat;

import java.io.IOException;
import java.util.Locale;

/**
 * @author Ralf Wondratschek
 */
@SuppressWarnings("ConstantConditions")
public class LogActivity extends BaseActivity implements SensorEventListener {

    private static SensorManager sensorManager = AndroidServices.getSensorManager();
    private static LocationProvider locationProvider = LocationProvider.getInstance();

    private Sensor mSensorAcceleration;
    private Sensor mSensorRotation;

    private TestWriter mTestWriter;
    private boolean mLogging;

    private float[] mRotationMatrix;

    private TextView mAccelerationX;
    private TextView mAccelerationY;
    private TextView mAccelerationZ;
    private TextView mRotationX;
    private TextView mRotationY;
    private TextView mRotationZ;
    private TextView mLatitude;
    private TextView mLongitude;

    private double[] mRotationVector;
    private float[] mAccelerationVector;
    private double[] mGpsVector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        mAccelerationX = (TextView) findViewById(R.id.textView_acceleration_x);
        mAccelerationY = (TextView) findViewById(R.id.textView_acceleration_y);
        mAccelerationZ = (TextView) findViewById(R.id.textView_acceleration_z);
        mRotationX = (TextView) findViewById(R.id.textView_rotation_x);
        mRotationY = (TextView) findViewById(R.id.textView_rotation_y);
        mRotationZ = (TextView) findViewById(R.id.textView_rotation_z);
        mLatitude = (TextView) findViewById(R.id.textView_gps_latitude);
        mLongitude = (TextView) findViewById(R.id.textView_gps_longitude);

        locationProvider.start(this);

        mRotationMatrix = new float[]{
                1f, 0f, 0f,
                0f, 1f, 0f,
                0f, 0f, 1f};

        mSensorAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorRotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        mAccelerationVector = new float[3];
        mRotationVector = new double[3];
        mGpsVector = new double[2];
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, mSensorAcceleration, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, mSensorRotation, SensorManager.SENSOR_DELAY_UI);

        mLongitude.setKeepScreenOn(true);
    }

    @Override
    public void onPause() {
        mLongitude.setKeepScreenOn(false);

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
        getMenuInflater().inflate(R.menu.main, menu);
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

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ROTATION_VECTOR:
                SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);

                double x = Math.atan2(mRotationMatrix[7], mRotationMatrix[8]);
                double y = Math.atan2(mRotationMatrix[6] * -1, Math.sqrt(Math.pow(mRotationMatrix[7], 2) + Math.pow(mRotationMatrix[8], 2)));
                double z = Math.atan2(mRotationMatrix[3], mRotationMatrix[0]);

                mRotationVector[0] = x;
                mRotationVector[1] = y;
                mRotationVector[2] = z;

                mRotationX.setText("Rot X: " + ((float)mRotationVector[0]));
                mRotationY.setText("Rot Y: " + ((float)mRotationVector[1]));
                mRotationZ.setText("Rot Z: " + ((float)mRotationVector[2]));

                break;

            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, mAccelerationVector, 0, 3);

                mAccelerationX.setText("Acc X: " + mAccelerationVector[0]);
                mAccelerationY.setText("Acc Y: " + mAccelerationVector[1]);
                mAccelerationZ.setText("Acc Z: " + mAccelerationVector[2]);
                break;
        }

        Location location = locationProvider.getLocation();
        if (location.getLatitude() != mGpsVector[0] || location.getLongitude() != mGpsVector[1]) {
            mLatitude.setText("Latitude: " + location.getLatitude());
            mLongitude.setText("Longitude: " + location.getLongitude());
            mGpsVector[0] = location.getLatitude();
            mGpsVector[1] = location.getLongitude();
        }

        if (mLogging) {
            mTestWriter.addEntry(new TestEvent(System.currentTimeMillis(), mAccelerationVector[0], mAccelerationVector[1], mAccelerationVector[2], mRotationVector[0], mRotationVector[1], mRotationVector[2], mGpsVector[0], mGpsVector[1]));
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
            mTestWriter = new TestWriter(this);
            mTestWriter.startWriting();
        } else {
            mTestWriter.stopWriting();
            mTestWriter = null;
        }
    }




    private static class TestEvent {

        public final long mTimeStamp;
        public final float mAccelerationX;
        public final float mAccelerationY;
        public final float mAccelerationZ;
        public final double mRotationX;
        public final double mRotationY;
        public final double mRotationZ;
        public final double mLatitude;
        public final double mLongitude;

        private TestEvent(long timeStamp, float accelerationX, float accelerationY, float accelerationZ, double rotationX, double rotationY, double rotationZ, double latitude, double longitude) {
            mTimeStamp = timeStamp;
            mAccelerationX = accelerationX;
            mAccelerationY = accelerationY;
            mAccelerationZ = accelerationZ;
            mRotationX = rotationX;
            mRotationY = rotationY;
            mRotationZ = rotationZ;
            mLatitude = latitude;
            mLongitude = longitude;
        }
    }





    public static class TestWriter extends AbstractCsvWriter<TestEvent> {

        public TestWriter(Context context) {
            super(context, "Log_" + System.currentTimeMillis() + ".csv");
        }

        @Override
        protected void writeEvent(TestEvent event) throws IOException {
            writeLine(String.format(Locale.GERMANY, "%d;%f;%f;%f;%f;%f;%f;%f;%f", event.mTimeStamp, event.mAccelerationX, event.mAccelerationY, event.mAccelerationZ, event.mRotationX, event.mRotationY, event.mRotationZ, event.mLatitude, event.mLongitude));
        }

        @Override
        protected String getHeader() {
            return "Timestamp;Acceleration X;Acceleration Y;Acceleration Z;Rotation X; Rotation Y;Rotation Z;Latitude;Longitude";
        }
    }
 }
