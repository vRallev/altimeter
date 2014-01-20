package net.vrallev.android.altimeter.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import net.vrallev.android.altimeter.R;
import net.vrallev.android.altimeter.activity.debug.DevelopmentActivity;
import net.vrallev.android.altimeter.location.LocationProvider;
import net.vrallev.android.base.BaseActivity;

/**
 * @author Ralf Wondratschek
 */
public class MainActivity extends BaseActivity {

    private static final LocationProvider LOCATION_PROVIDER = LocationProvider.getInstance();

    private TextView mTextViewCarPosition;
    private TextView mTextViewDevicePosition;

    private InitializeCarPositionActivity.CarPositionResult mCarPositionResult;
    private InitializeDevicePositionActivity.DevicePositionResult mDevicePositionResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.button_init_car_position:
                        startActivityForResult(new Intent(MainActivity.this, InitializeCarPositionActivity.class), InitializeCarPositionActivity.REQUEST_CODE);
                        break;

                    case R.id.button_init_device_position:
                        startActivityForResult(new Intent(MainActivity.this, InitializeDevicePositionActivity.class), InitializeDevicePositionActivity.REQUEST_CODE);
                        break;

                    case R.id.button_measure_height:
                        startActivity(HeightMeasurementActivity.createIntent(MainActivity.this, mCarPositionResult, mDevicePositionResult));
                        break;
                }
            }
        };

        findViewById(R.id.button_init_car_position).setOnClickListener(onClickListener);
        findViewById(R.id.button_init_device_position).setOnClickListener(onClickListener);
        findViewById(R.id.button_measure_height).setOnClickListener(onClickListener);

        mTextViewCarPosition = (TextView) findViewById(R.id.textView_car_position);
        mTextViewDevicePosition = (TextView) findViewById(R.id.textView_device_position);

        LOCATION_PROVIDER.start(this);

        onCarPositionResult(RESULT_OK, InitializeCarPositionActivity.CarPositionResult.DEFAULT);
        onDevicePositionResult(RESULT_OK, InitializeDevicePositionActivity.DevicePositionResult.DEFAULT);
    }

    @Override
    public void onBackPressed() {
        LOCATION_PROVIDER.stop();
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_debug:
                startActivity(new Intent(this, DevelopmentActivity.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case InitializeCarPositionActivity.REQUEST_CODE:
                onCarPositionResult(resultCode, InitializeCarPositionActivity.CarPositionResult.fromIntent(data));
                break;

            case InitializeDevicePositionActivity.REQUEST_CODE:
                onDevicePositionResult(resultCode, InitializeDevicePositionActivity.DevicePositionResult.fromIntent(data));
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void onCarPositionResult(int resultCode, InitializeCarPositionActivity.CarPositionResult result) {
        if (resultCode == RESULT_OK) {
            mCarPositionResult = result;
            mTextViewCarPosition.setText(getString(R.string.ascent, Math.toDegrees(result.getAscent())));
        }
    }

    private void onDevicePositionResult(int resultCode, InitializeDevicePositionActivity.DevicePositionResult result) {
        if (resultCode == RESULT_OK) {
            mDevicePositionResult = result;
            mTextViewDevicePosition.setText(getString(R.string.rotation_degree, result.getDegree()));
        }
    }
}
