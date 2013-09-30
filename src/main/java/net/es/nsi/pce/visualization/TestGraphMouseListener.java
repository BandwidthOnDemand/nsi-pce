package net.es.nsi.pce.visualization;

import edu.uci.ics.jung.visualization.control.GraphMouseListener;
import java.awt.event.MouseEvent;

/**
 * A nested class to demo the GraphMouseListener finding the right vertices
 * after zoom/pan.  Not used at the moment but put in as a future hook.
 */
class TestGraphMouseListener<Network> implements GraphMouseListener<Network> {

    /**
     * This method is fired after a successful pressed and released event.
     * 
     * @param v The network vertex clicked by the user.
     * @param me Holds the current coordinates of the network vertex.
     */
    @Override
    public void graphClicked(Network v, MouseEvent me) {
        System.err.println("Vertex " + v + " was clicked at (" + me.getX() + "," + me.getY() + ")");
    }

    /**
     * This method is fired when a user presses a network vertex.
     * 
     * @param v The network vertex clicked by the user.
     * @param me Holds the current coordinates of the network vertex.
     */
    @Override
    public void graphPressed(Network v, MouseEvent me) {
        System.err.println("Vertex " + v + " was pressed at (" + me.getX() + "," + me.getY() + ")");
    }

    /**
     * This method is fired when a user releases a click on a network vertex.
     * 
     * @param v The network vertex clicked by the user.
     * @param me Holds the current coordinates of the network vertex.
     */
    @Override
    public void graphReleased(Network v, MouseEvent me) {
        System.err.println("Vertex " + v + " was released at (" + me.getX() + "," + me.getY() + ")");
    }
    
}
