package net.vrallev.android.altimeter.activity.debug;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import net.vrallev.android.altimeter.R;
import net.vrallev.android.altimeter.activity.fragment.AbstractSensorFragment;
import net.vrallev.android.altimeter.activity.fragment.AccelerometerFragment;
import net.vrallev.android.altimeter.activity.fragment.GravityFragment;
import net.vrallev.android.altimeter.activity.fragment.GyroscopeFragment;
import net.vrallev.android.altimeter.activity.fragment.LinearAccelerationFragment;
import net.vrallev.android.altimeter.activity.fragment.RotationMatrixFragment;
import net.vrallev.android.altimeter.activity.fragment.RotationVectorFragment;
import net.vrallev.android.base.BaseActivity;

@SuppressWarnings("ConstantConditions")
public class DevelopmentActivity extends BaseActivity {

    private boolean mLogging;
    private int mPosition;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_development);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                setLoggingEnabled(false, mPosition);
                mPosition = position;
            }
        });
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
                setLoggingEnabled(true, mPosition);
                return true;

            case R.id.action_log_stop:
                setLoggingEnabled(false, mPosition);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setLoggingEnabled(boolean enabled, int position) {
        if (mLogging == enabled) {
            return;
        }

        Object fragment = mSectionsPagerAdapter.instantiateItem(mViewPager, position);
        if (fragment instanceof AbstractSensorFragment) {
            ((AbstractSensorFragment) fragment).setLoggingEnabled(enabled);
        }

        mLogging = enabled;
        invalidateOptionsMenu();
    }

    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new AccelerometerFragment();
                case 1:
                    return new GravityFragment();
                case 2:
                    return new LinearAccelerationFragment();
                case 3:
                    return new GyroscopeFragment();
                case 4:
                    return new RotationVectorFragment();
                case 5:
                    return new RotationMatrixFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            return 6;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.accelerometer).toUpperCase();
                case 1:
                    return getString(R.string.gravity).toUpperCase();
                case 2:
                    return getString(R.string.linear_acceleration).toUpperCase();
                case 3:
                    return getString(R.string.gyroscope).toUpperCase();
                case 4:
                    return getString(R.string.rotation_vector).toUpperCase();
                case 5:
                    return getString(R.string.rotation_matrix_rot).toUpperCase();
            }
            return null;
        }
    }
}
