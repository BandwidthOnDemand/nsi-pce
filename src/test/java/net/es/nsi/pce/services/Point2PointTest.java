package net.es.nsi.pce.services;

import java.util.Set;
import net.es.nsi.pce.api.jaxb.DirectionalityType;
import net.es.nsi.pce.api.jaxb.ObjectFactory;
import net.es.nsi.pce.api.jaxb.P2PServiceBaseType;
import net.es.nsi.pce.pf.api.cons.BooleanAttrConstraint;
import net.es.nsi.pce.pf.api.cons.Constraints;
import net.es.nsi.pce.pf.api.cons.Constraint;
import net.es.nsi.pce.pf.api.cons.NumAttrConstraint;
import net.es.nsi.pce.pf.api.cons.StringAttrConstraint;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

/**
 * A simple test to verify correct operation of the Service mappings.
 * 
 * @author hacksaw
 */
public class Point2PointTest {
    @Test
    public void test() {
        ObjectFactory factory = new ObjectFactory();
        P2PServiceBaseType p2ps = factory.createP2PServiceBaseType();
        p2ps.setCapacity(100L);
        p2ps.setDirectionality(DirectionalityType.BIDIRECTIONAL);
        p2ps.setSymmetricPath(Boolean.TRUE);
        p2ps.setSourceSTP("urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:uva:1?vlan=1784");
        p2ps.setDestSTP("urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:pionier:1?vlan=1784");
        
        Point2Point p2p = new Point2Point();
        Set<Constraint> constraints = p2p.addConstraints(p2ps);
        
        Constraints attr = new Constraints(constraints);
        
        NumAttrConstraint capacity = attr.removeNumAttrConstraint(Point2Point.CAPACITY);
        assertNotNull(capacity);
        assertEquals(new Long(100L), capacity.getValue());
        
        StringAttrConstraint directionality = attr.getStringAttrConstraint(Point2Point.DIRECTIONALITY);
        assertNotNull(directionality);
        assertEquals(DirectionalityType.BIDIRECTIONAL, DirectionalityType.valueOf(directionality.getValue()));
        
        BooleanAttrConstraint symmetricPath = attr.getBooleanAttrConstraint(Point2Point.SYMMETRICPATH);
        assertNotNull(symmetricPath);
        assertEquals(Boolean.TRUE, symmetricPath.getValue());
        
        StringAttrConstraint sourceSTP = attr.getStringAttrConstraint(Point2Point.SOURCESTP);
        assertNotNull(sourceSTP);
        assertEquals("urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:uva:1?vlan=1784", sourceSTP.getValue());
        
        StringAttrConstraint destSTP = attr.getStringAttrConstraint(Point2Point.DESTSTP);
        assertNotNull(destSTP);
        assertEquals("urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:pionier:1?vlan=1784", destSTP.getValue());        
    
        StringAttrConstraint vlan = attr.getStringAttrConstraint(Point2Point.VLAN);
        assertNull(vlan);
    }
}
