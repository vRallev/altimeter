package net.vrallev.android.altimeter.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;

import net.vrallev.android.altimeter.R;
import net.vrallev.android.altimeter.file.AbstractCsvWriter;
import net.vrallev.android.altimeter.location.LocationProvider;
import net.vrallev.android.altimeter.location.LocationUtil;
import net.vrallev.android.altimeter.view.HeightDrawerView;
import net.vrallev.android.base.BaseActivity;
import net.vrallev.android.base.util.AndroidServices;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * @author Ralf Wondratschek
 */
@SuppressWarnings("ConstantConditions")
public class HeightMeasurementActivity extends BaseActivity implements SensorEventListener {

    private static final Gson GSON = new Gson();

    public static Intent createIntent(Context context, InitializeCarPositionActivity.CarPositionResult carPositionResult, InitializeDevicePositionActivity.DevicePositionResult devicePositionResult) {
        Intent result = new Intent(context, HeightMeasurementActivity.class);
        result.putExtra(InitializeCarPositionActivity.CarPositionResult.class.getName(), GSON.toJson(carPositionResult));
        result.putExtra(InitializeDevicePositionActivity.DevicePositionResult.class.getName(), GSON.toJson(devicePositionResult));
        return result;
    }

    private static final SensorManager SENSOR_MANAGER = AndroidServices.getSensorManager();
    private static final LocationProvider LOCATION_PROVIDER = LocationProvider.getInstance();

    private Sensor mSensor;

    private TestWriterAll mTestWriter;
    private boolean mLogging;

    private Location mOldLocation;

    private float[] mRotationMatrix;
    private double[] mStoredRotation;
    private int mStoredRotationIndex;

    private HeightDrawerView mHeightDrawerView;
    private Button mButtonStart;
    private Button mButtonStop;
    private TextView mTextViewHeight;

    private double mHeightSum;

    private double mXOffset;
    private double mYOffset;
    private double mInitialX;
    private double mInitialY;
    private double mXPercentage;
    private double mYPercentage;

    private InitializeCarPositionActivity.CarPositionResult mCarPosition;
    private InitializeDevicePositionActivity.DevicePositionResult mDevicePosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_height_measurement);

        mCarPosition = GSON.fromJson(getIntent().getStringExtra(InitializeCarPositionActivity.CarPositionResult.class.getName()), InitializeCarPositionActivity.CarPositionResult.class);
        mDevicePosition = GSON.fromJson(getIntent().getStringExtra(InitializeDevicePositionActivity.DevicePositionResult.class.getName()), InitializeDevicePositionActivity.DevicePositionResult.class);

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

        mRotationMatrix = new float[]{
                1f, 0f, 0f,
                0f, 1f, 0f,
                0f, 0f, 1f};

        mStoredRotation = new double[1024];

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
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }

    private void startTracking() {
        mStoredRotationIndex = 0;
        mXOffset = Double.MAX_VALUE;
        mYOffset = Double.MAX_VALUE;
        mInitialX = Double.MAX_VALUE;
        mInitialY = Double.MAX_VALUE;

        mYPercentage = Math.abs(mDevicePosition.getDegree()) / 90;
        mXPercentage = 1 - mYPercentage;

        mButtonStart.setVisibility(View.GONE);
        mButtonStop.setVisibility(View.VISIBLE);

        setLoggingEnabled(true);
    }

    private void stopTracking() {
        mButtonStart.setVisibility(View.VISIBLE);
        mButtonStop.setVisibility(View.GONE);

        final File file = mTestWriter.getFile();

        setLoggingEnabled(false);

        mStoredRotationIndex = 0;
        mHeightSum = 0;
        mHeightDrawerView.resetHeight();
        mTextViewHeight.setText("");

        new AlertDialog.Builder(this)
                .setTitle(R.string.share_log)
                .setMessage(getString(R.string.share_log_message, file.getAbsolutePath()))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                        intent.setType("text/csv");
                        startActivity(Intent.createChooser(intent, getResources().getString(R.string.share_log)));
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
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

        if (mXOffset > Math.PI || mXOffset < -Math.PI || mYOffset > Math.PI || mYOffset < -Math.PI) {
            mXOffset = x;
            mYOffset = y;
            mInitialX = mCarPosition.getInitialXRotation(z);
            mInitialY = mCarPosition.getInitialYRotation(z);
        }

        mStoredRotation[mStoredRotationIndex] = (x - mXOffset + mInitialX) * mXPercentage + (y - mYOffset + mInitialY) * mYPercentage;
        mStoredRotationIndex++;
        checkArraySize();

        Location location = LOCATION_PROVIDER.getLocation();
        double distance = 0;
        double height = 0;
        double angle = 0;

        if (mOldLocation != null && !mOldLocation.equals(location)) {
            distance = LocationUtil.getDistanceInKm(location, mOldLocation);

            // 10m
            if (distance > 0.01) {
                final double distanceSection = distance / mStoredRotationIndex;

                for (int i = 0; i < mStoredRotationIndex; i++) {
                    height += Math.tan(mStoredRotation[i]) * distanceSection;
                }

                mHeightSum += height;

                angle = Math.toDegrees(Math.atan(height / distance));

                mHeightDrawerView.insertHeight(mHeightSum, distance);
                mTextViewHeight.setText(getString(R.string.height_sum, mHeightSum * 1000));

                mStoredRotationIndex = 0;

            } else {
                distance = 0;
                location = mOldLocation;
                // ignore new position and remember old one
            }
        }

        mOldLocation = location;

        if (mLogging) {
            mTestWriter.addEntry(new TestEvent(System.currentTimeMillis(), location.getLongitude(), location.getLatitude(), distance, angle, x, y, z, height, mHeightSum));
        }
    }

    private void setLoggingEnabled(boolean enabled) {
        if (mLogging == enabled) {
            return;
        }

        mLogging = enabled;
        invalidateOptionsMenu();

        if (enabled) {
            mTestWriter = new TestWriterAll(this);
            mTestWriter.startWriting();
        } else {
            mTestWriter.stopWriting();
            mTestWriter = null;
        }
    }

    private void checkArraySize() {
        if (mStoredRotationIndex == mStoredRotation.length) {
            double[] array = new double[mStoredRotation.length * 2];
            System.arraycopy(mStoredRotation, 0, array, 0, mStoredRotation.length);
            mStoredRotation = array;
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
        public final double mAngle;

        public TestEvent(long timeStamp, double longitude, double latitude, double distance, double angle, double XRotation, double YRotation, double ZRotation, double heightDif, double heightSum) {
            mTimeStamp = timeStamp;
            mLongitude = longitude;
            mDistance = distance;
            mLatitude = latitude;
            mXRotation = XRotation;
            mYRotation = YRotation;
            mZRotation = ZRotation;
            mHeightDif = heightDif;
            mHeightSum = heightSum;
            mAngle = angle;
        }
    }


    public static class TestWriterAll extends AbstractCsvWriter<TestEvent> {

        public TestWriterAll(Context context) {
            super(context, "Test_" + System.currentTimeMillis() + ".csv");
        }

        @Override
        protected void writeEvent(TestEvent event) throws IOException {
            writeLine(String.format(Locale.GERMANY, "%d;%f;%f;%f;%f;%f;%f;%f;%f;%f", event.mTimeStamp, event.mLongitude, event.mLatitude, event.mDistance, event.mAngle, event.mXRotation, event.mYRotation, event.mZRotation, event.mHeightDif, event.mHeightSum));
        }

        @Override
        protected String getHeader() {
            return "Timestamp;Longitude;Latitude;Distance;Angle (Deg);X-Rotation;Y-Rotation;Z-Rotation;Height Dif;Height Sum";
        }
    }
}
