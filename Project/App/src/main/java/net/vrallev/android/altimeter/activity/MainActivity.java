package net.vrallev.android.altimeter.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import net.vrallev.android.altimeter.R;
import net.vrallev.android.altimeter.activity.debug.DevelopmentActivity;
import net.vrallev.android.base.BaseActivity;

/**
 * @author Ralf Wondratschek
 */
public class MainActivity extends BaseActivity {

    private TextView mTextViewCarPosition;
    private TextView mTextViewDevicePosition;

    private InitializeCarPositionActivity.CarPositionResult mCarPositionResult;

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

                        break;

                    case R.id.button_measure_height:

                        break;
                }
            }
        };

        findViewById(R.id.button_init_car_position).setOnClickListener(onClickListener);
        findViewById(R.id.button_init_device_position).setOnClickListener(onClickListener);
        findViewById(R.id.button_measure_height).setOnClickListener(onClickListener);

        mTextViewCarPosition = (TextView) findViewById(R.id.textView_car_position);
        mTextViewDevicePosition = (TextView) findViewById(R.id.textView_device_position);

        onCarPositionResult(RESULT_OK, InitializeCarPositionActivity.CarPositionResult.DEFAULT);
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
}
