/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.visualization;

import java.util.HashMap;

/**
 *
 * @author hacksaw
 */
public class NsaMap {
    private static final HashMap<String, CartesianCoordinates> map = new HashMap<String, CartesianCoordinates>() {
        private static final long serialVersionUID = 1L;
        {
            // Top row.
            this.put("urn:ogf:network:ampath.net:2013:nsa", new CartesianCoordinates(400, 50));
            this.put("urn:ogf:network:nordu.net:2013:nsa", new CartesianCoordinates(600, 50));
            this.put("urn:ogf:network:uvalight.net:2013:nsa", new CartesianCoordinates(800, 50));
            this.put("urn:ogf:network:surfnet.nl:1990:nsa:bod", new CartesianCoordinates(1000, 50));
            this.put("urn:ogf:network:southernlight.net:2013:nsa", new CartesianCoordinates(200, 50));
            
            // Second row.
            this.put("urn:ogf:network:singaren.net:2013:nsa", new CartesianCoordinates(50, 550));
            this.put("urn:ogf:network:es.net:2013:nsa", new CartesianCoordinates(200, 350));            
            this.put("urn:ogf:network:manlan.internet2.edu:2013:nsa", new CartesianCoordinates(400, 350));
            this.put("urn:ogf:network:netherlight.net:2013:nsa:bod", new CartesianCoordinates(800, 350));
            this.put("urn:ogf:network:czechlight.cesnet.cz:2013:nsa:bod", new CartesianCoordinates(1000, 350));
            
            // Third row.
            this.put("urn:ogf:network:kddilabs.jp:2013:nsa", new CartesianCoordinates(50, 750));
            this.put("urn:ogf:network:jgn-x.jp:2013:nsa", new CartesianCoordinates(200, 750));
            this.put("urn:ogf:network:icair.org:2013:nsa", new CartesianCoordinates(500, 550));
            this.put("urn:ogf:network:pionier.net.pl:2013:nsa", new CartesianCoordinates(1000, 550));
            
            // Row four.
            this.put("urn:ogf:network:aist.go.jp:2013:nsa", new CartesianCoordinates(400, 750));
            this.put("urn:ogf:network:krlight.net:2013:nsa", new CartesianCoordinates(600, 750));
            this.put("urn:ogf:network:geant.net:2013:nsa", new CartesianCoordinates(800, 750));
            this.put("urn:ogf:network:grnet.gr:2013:nsa", new CartesianCoordinates(1000, 750));
        }
    };

    /**
     * @return the map
     */
    public static HashMap<String, CartesianCoordinates> getMap() {
        return map;
    }
    
    public static CartesianCoordinates getCoordinates(String nsaId) {
        CartesianCoordinates coords = map.get(nsaId);
        if (coords == null) {
            coords = new CartesianCoordinates();
        }
        return coords;
    }    
}
