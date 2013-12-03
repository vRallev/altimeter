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
public class RotationTestActivity extends BaseActivity implements SensorEventListener {

    private static SensorManager sensorManager = AndroidServices.getSensorManager();
    private static LocationProvider locationProvider = LocationProvider.getInstance();

    private Sensor mSensor;
    private TestWriter mTestWriter;

    private Button mButton;
    private boolean mLogging;

    private Location mOldLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rotation_test);

        locationProvider.start(this);

        mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mTestWriter = new TestWriter(this);

        mButton = (Button) findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLogging) {
                    mTestWriter.stopWriting();
                    mButton.setText(R.string.log);
                } else {
                    mTestWriter.startWriting();
                    mButton.setText(R.string.stop);
                }

                mLogging = !mLogging;
            }
        });
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
    public void onSensorChanged(SensorEvent event) {
        Location location = locationProvider.getLocation();
        double distance = 0;

        if (mOldLocation != null && !mOldLocation.equals(location)) {
            distance = LocationUtil.getDistanceInKm(location, mOldLocation);
        }

        mOldLocation = location;

        mTestWriter.addEntry(new TestEvent(System.currentTimeMillis(), location.getLongitude(), location.getLatitude(), distance, event.values[0], event.values[1], event.values[2]));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Cat.i("onAccuracyChanged");
    }

    public static class TestEvent {

        public final long mTimeStamp;
        public final float mXRotation;
        public final float mYRotation;
        public final float mZRotation;
        public final double mLongitude;
        public final double mLatitude;
        public final double mDistance;

        public TestEvent(long timeStamp, double longitude, double latitude, double distance, float XRotation, float YRotation, float ZRotation) {
            mTimeStamp = timeStamp;
            mLongitude = longitude;
            mDistance = distance;
            mLatitude = latitude;
            mXRotation = XRotation;
            mYRotation = YRotation;
            mZRotation = ZRotation;
        }
    }

    public static class TestWriter extends AbstractCsvWriter<TestEvent> {

        public TestWriter(Context context) {
            super(context, "Test_" + System.currentTimeMillis() + ".csv");
        }

        @Override
        protected void writeEvent(TestEvent event) throws IOException {
            writeLine(String.format(Locale.GERMANY, "%d;%f;%f;%f;%f;%f;%f", event.mTimeStamp, event.mLongitude, event.mLatitude, event.mDistance, event.mXRotation, event.mYRotation, event.mZRotation));
        }

        @Override
        protected String getHeader() {
            return "Timestamp;Longitude;Latitude;Distance;X-Rotation;Y-Rotation;Z-Rotation";
        }
    }
}
