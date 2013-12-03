package net.vrallev.android.altimeter.location;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.Settings;

import net.vrallev.android.altimeter.BuildConfig;
import net.vrallev.android.altimeter.R;
import net.vrallev.android.base.App;
import net.vrallev.android.base.util.AndroidServices;
import net.vrallev.android.base.util.Cat;

/**
 * @author Ralf Wondratschek
 */
@SuppressWarnings({"ConstantConditions", "UnusedDeclaration"})
public class LocationProvider {

    private static final int TWO_MINUTES = 1000 * 60 * 2;

    private static final int STOP_WITH_DELAY = 4854;
    private static final long DELAY = 1000l;

    private static LocationManager locationManager;
    private static LocationProvider instance;

    public static LocationProvider getInstance() {
        if (instance == null) {
            instance = new LocationProvider();
        }
        return instance;
    }

    private Activity mActivity;
    private Activity mLastVisibleActivity;

    private Handler mHandler;
    private boolean mMonitoring;

    private Location mLocation;
    private boolean mDialogVisible;

    private LocationProvider() {
        if (locationManager == null) {
            locationManager = AndroidServices.getLocationManager();
        }
    }

    public void start(Activity activity) {
        if (isRunning()) {
            mActivity = activity;
            return;
        }

        HandlerThread handlerThread = new HandlerThread(LocationProvider.class.getName());
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                stopMonitoringLocation();
            }
        };

        mActivity = activity;
        App.getInstance().registerActivityLifecycleCallbacks(mActivityLifecycleCallbacksAdapter);

        startMonitoringLocation(activity);
    }

    public void stop() {
        if (!isRunning()) {
            return;
        }

        App.getInstance().unregisterActivityLifecycleCallbacks(mActivityLifecycleCallbacksAdapter);
        stopMonitoringLocation();
        mActivity = null;

        mHandler.getLooper().quit();
        mHandler = null;
    }

    public boolean isRunning() {
        return mActivity != null;
    }

    public Location getLocation() {
        return mLocation;
    }

    private void startMonitoringLocation(Context context) {
        if (mMonitoring) {
            return;
        }

        mMonitoring = true;
        Cat.d("Start monitoring location.");

        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!gpsEnabled) {
            showGpsDisabledDialog(context);
        }

        Criteria criteria = new Criteria();
        String bestProvider = locationManager.getBestProvider(criteria, false);
        mLocation = getBetterLocation(locationManager.getLastKnownLocation(bestProvider), mLocation);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
        if (BuildConfig.DEBUG) {
            if (locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
            }

        } else {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
        }
    }

    private void stopMonitoringLocation() {
        if (!mMonitoring) {
            return;
        }

        mMonitoring = false;
        Cat.d("Stop monitoring location.");

        locationManager.removeUpdates(mLocationListener);
    }

    private void showGpsDisabledDialog(final Context context) {
        if (mDialogVisible) {
            return;
        }

        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        break;
                }
            }
        };

        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setCancelable(true)
                .setTitle(R.string.gps_disabled)
                .setMessage(R.string.gps_disabled_msg)
                .setPositiveButton(R.string.enable, onClickListener)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mDialogVisible = false;
            }
        });

        alertDialog.show();

        mDialogVisible = true;
    }

    private App.ActivityLifecycleCallbacksAdapter mActivityLifecycleCallbacksAdapter = new App.ActivityLifecycleCallbacksAdapter() {

        @Override
        public void onActivityStarted(Activity activity) {
            mHandler.removeMessages(STOP_WITH_DELAY);
            startMonitoringLocation(activity);
        }

        @Override
        public void onActivityResumed(Activity activity) {
            mLastVisibleActivity = activity;
        }

        @Override
        public void onActivityPaused(Activity activity) {
            mLastVisibleActivity = null;
        }

        @Override
        public void onActivityStopped(Activity activity) {
            if (mLastVisibleActivity == null) {
                mHandler.sendEmptyMessageDelayed(STOP_WITH_DELAY, DELAY);
            }
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            if (activity.equals(mActivity)) {
                mHandler.sendEmptyMessageDelayed(STOP_WITH_DELAY, DELAY);
            }
        }
    };

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
//            Cat.d("Location changed");
            mLocation = getBetterLocation(location, mLocation);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Cat.d("Status changed %s %d", provider, status);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Cat.d("Provider enabled %s", provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Cat.d("Provider disabled %s", provider);
            if (provider.equals(LocationManager.GPS_PROVIDER) && mLastVisibleActivity != null) {
                showGpsDisabledDialog(mLastVisibleActivity);
            }
        }
    };

    /**
     * Determines whether one Location reading is better than the current Location fix
     *
     * @param location            The new Location that you want to evaluate
     * @param currentBestLocation The current Location fix, to which you want to compare the new one
     */
    protected Location getBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return location;
        }

        if (location == null) {
            return mLocation;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return location;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return currentBestLocation;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return location;
        } else if (isNewer && !isLessAccurate) {
            return location;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return location;
        }
        return currentBestLocation;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}
