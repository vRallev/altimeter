package net.vrallev.android.altimeter.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.vrallev.android.altimeter.R;
import net.vrallev.android.altimeter.activity.fragment.AccelerometerFragment;
import net.vrallev.android.altimeter.activity.fragment.GravityFragment;
import net.vrallev.android.altimeter.activity.fragment.GyroscopeFragment;
import net.vrallev.android.altimeter.activity.fragment.LinearAccelerationFragment;
import net.vrallev.android.altimeter.activity.fragment.RotationVectorFragment;
import net.vrallev.android.base.BaseActivity;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        /*
         * http://developer.android.com/guide/topics/sensors/sensors_overview.html
         * CHECK Accelerometer
         * CHECK Gravity
         * CHECK Gyroscope
         * CHECK Linear Acceleration
         * Orientation -> Deprecated
         * CHECK Rotation Vector
         */
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

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
            }
            return null;
        }

        @Override
        public int getCount() {
            return 5;
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
            }
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

}
