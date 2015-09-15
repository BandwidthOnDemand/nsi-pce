package net.es.nsi.pce.visualization;

import java.awt.Color;

/**
 *
 * @author hacksaw
 */
public class Display {
    public static final int maxX = 1200;
    public static final int maxY = 1000;

    public static final float centerX = maxX / 2;
    public static final float centerY = maxY / 2;

    public static final Color backgroundColor = Color.WHITE;

    static public int getX(float longitude) {
        return Math.round(centerX + ((longitude * centerX) / 180.0f));
    }

    static public int getY(float latitude) {
        return Math.round(centerY + ((latitude * centerY) / 180.0f));
    }

    /**
     * Get the coordinates associated with the specified network.
     *
     * @param longitude
     * @param latitude
     * @return Coordinates of network.
     */
    public static CartesianCoordinates getCoordinates(float longitude, float latitude) {
        return new CartesianCoordinates(getX(longitude), getY(latitude));
    }

    public static CartesianCoordinates getRandomCoordinates() {
        return getCoordinates(
                        (float) getRandomInRange(-180, 180),
                        (float) getRandomInRange(-180, 180)
                    );
    }

    public static double getRandomInRange(double from, double to) {
        return Math.random() * (to - from) + from;
    }
}
