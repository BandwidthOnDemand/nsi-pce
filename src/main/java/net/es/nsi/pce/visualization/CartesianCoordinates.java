package net.es.nsi.pce.visualization;

import java.util.Random;

/**
 * A simple class modeling a pair of Cartesian coordinates for positioning
 * a an X and Y axis of a graph.
 * 
 * @author hacksaw
 */
public class CartesianCoordinates {
    private int x = 0;
    private int y = 0;
    
    /**
     * Default constructor with no parameters.  Will create a random set of
     * X and X coordinates.
     */
    Random number = new Random(System.currentTimeMillis());
    public CartesianCoordinates() {
        this.x = number.nextInt() % Display.maxX + 1;
        this.y = number.nextInt() % Display.maxY + 1;
    }
    
    /**
     * Constructor with X and Y coordinates parameterized.
     * 
     * @param x
     * @param y 
     */
    public CartesianCoordinates(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Get the X coordinate.
     * 
     * @return the x coordinate.
     */
    public int getX() {
        return x;
    }

    /**
     * Set the X coordinate.
     * 
     * @param x the x to set
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Get the Y coordinate.
     * 
     * @return the y coordinate.
     */
    public int getY() {
        return y;
    }

    /**
     * Set the Y coordinate.
     * 
     * @param y the y to set.
     */
    public void setY(int y) {
        this.y = y;
    }
    
}
