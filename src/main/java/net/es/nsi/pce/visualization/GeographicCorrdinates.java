package net.es.nsi.pce.visualization;

import java.awt.geom.Point2D;

/**
 * A class modeling geographic coordinates on a map.
 * 
 * @author hacksaw
 */
public class GeographicCorrdinates {
    private double latitude = 0;
    private double longitude = 0;
    
    /**
     * Default constructor with no parameters.
     */
    public GeographicCorrdinates() {}
    
    /**
     * Constructor for creating GeographicCorrdinates with a specific latitude
     * and longitude.
     * 
     * @param latitude Latitude component of coordinate.
     * @param longitude Longitude component of coordinate.
     */
    public GeographicCorrdinates(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Getter for latitude.
     * 
     * @return the latitude
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Setter for latitude.
     * 
     * @param latitude the latitude to set
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * Getter for longitude.
     * 
     * @return the longitude
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Setter for longitude.
     * 
     * @param longitude the longitude to set
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    /**
     * Transform a coordinate into a point on a graphic map.
     * 
     * @return A two dimensional point on a graphic map.
     */
    public Point2D transform() {
        double latitude = this.latitude;
        double longitude = this.longitude;

        System.out.print("Before latitude=" + latitude + ", longitude=" + longitude);

        latitude *= Display.maxY/180f;
        longitude *= Display.maxX/360f;

        if (latitude > 0) {
            latitude = Display.maxY / 2 - latitude;
        }
        else {
            latitude = Display.maxY / 2 + Math.abs(latitude);
        }

        if (longitude > 0) {
            longitude = Display.maxX / 2 - Math.abs(latitude);
        }
        else {
            longitude = Display.maxX / 2 + longitude;
        }

        System.out.print("After latitude=" + latitude + ", longitude=" + longitude);
        
        return new Point2D.Double(longitude,latitude);
    }
}
