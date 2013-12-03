package net.vrallev.android.altimeter.activity;

import android.os.Bundle;

import net.vrallev.android.altimeter.location.LocationProvider;
import net.vrallev.android.base.BaseActivity;

/**
 * @author Ralf Wondratschek
 */
public class RotationTestActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocationProvider.getInstance().start(this);
    }
}
