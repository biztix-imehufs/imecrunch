package com.hufs.ime.imecrunch;

/**
 * Created by imehufs on 4/12/2016.
 */
public class BandPublicInfo {
    private static double[] currentAccelerometer = new double[3];
    public static double[] getCurrentAccelerometer() {
        return currentAccelerometer;
    }
    public static void setCurrentAccelerometer(double x, double y, double z) {
        currentAccelerometer[0] = x;
        currentAccelerometer[1] = y;
        currentAccelerometer[2] = z;
    }
}
