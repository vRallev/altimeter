package net.vrallev.android.altimeter.location;

import android.location.Location;

/**
 * @author Ralf Wondratschek
 */
public final class LocationUtil {

    public static final int RADIUS_EARTH = 6371;

    private LocationUtil() {
        // no op
    }

    /**
     * http://www.movable-type.co.uk/scripts/latlong.html
     *
     * Haversine formula
     *
     * @param locationOne
     * @param locationTwo
     * @return
     */
    public static double getDistanceInKm(Location locationOne, Location locationTwo) {
        double deltaLat = deg2Rad(locationTwo.getLatitude() - locationOne.getLatitude());
        double deltaLong = deg2Rad(locationTwo.getLongitude() - locationOne.getLongitude());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) + Math.cos(deg2Rad(locationOne.getLatitude())) * Math.cos(deg2Rad(locationTwo.getLatitude()))
                * Math.sin(deltaLong / 2) * Math.sin(deltaLong / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RADIUS_EARTH * c;
     }

    private static double deg2Rad(double value) {
        return value * (Math.PI / 180);
    }
}
