package net.es.nsi.pce.visualization;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import org.apache.commons.collections15.Transformer;

/**
 * Converts a Cartesian coordinate to a two dimension point on a graph (yes the
 * x and y coordinate is already 2D but this is a hook from when it was a
 * geographical coord).
 * 
 * @author hacksaw
 */
class XYPixelTransformer implements Transformer<CartesianCoordinates, Point2D> {
    private Dimension d; // Not used at the moment.  Dimension is set globally.

    public XYPixelTransformer(Dimension d) {
        this.d = d;
    }

    /**
     * Transform an x and y coordinate to a 2 dimensional position on the
     * map.
     */
    @Override
    public Point2D transform(CartesianCoordinates coords) {
        return new Point2D.Double(coords.getX(), coords.getY());
    }

    /**
     * @return the d
     */
    public Dimension getD() {
        return d;
    }

    /**
     * @param d the d to set
     */
    public void setD(Dimension d) {
        this.d = d;
    }
    
}
