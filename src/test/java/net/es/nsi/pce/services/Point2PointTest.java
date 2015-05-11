package net.es.nsi.pce.services;

import net.es.nsi.pce.path.services.Point2Point;
import java.util.Set;
import net.es.nsi.pce.path.jaxb.DirectionalityType;
import net.es.nsi.pce.path.jaxb.P2PServiceBaseType;
import net.es.nsi.pce.path.services.EthernetTypes;
import net.es.nsi.pce.path.services.Point2PointTypes;
import net.es.nsi.pce.pf.api.cons.BooleanAttrConstraint;
import net.es.nsi.pce.pf.api.cons.AttrConstraints;
import net.es.nsi.pce.pf.api.cons.Constraint;
import net.es.nsi.pce.pf.api.cons.NumAttrConstraint;
import net.es.nsi.pce.pf.api.cons.StringAttrConstraint;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;

import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;

/**
 * A simple test to verify correct operation of the Service mappings.
 *
 * @author hacksaw
 */
public class Point2PointTest {

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test() {
        P2PServiceBaseType mockedP2ps = mock(P2PServiceBaseType.class);
        when(mockedP2ps.getCapacity()).thenReturn(100L);
        when(mockedP2ps.getDirectionality()).thenReturn(DirectionalityType.BIDIRECTIONAL);
        when(mockedP2ps.isSymmetricPath()).thenReturn(Boolean.TRUE);
        when(mockedP2ps.getSourceSTP()).thenReturn("urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:uva:1?vlan=1784");
        when(mockedP2ps.getDestSTP()).thenReturn("urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:pionier:1?vlan=1784");

        Point2Point p2p = new Point2Point();
        Set<Constraint> constraints = p2p.addConstraints(mockedP2ps);

        AttrConstraints attr = new AttrConstraints(constraints);

        NumAttrConstraint capacity = attr.removeNumAttrConstraint(Point2PointTypes.CAPACITY);
        assertNotNull(capacity);
        assertEquals(new Long(100L), capacity.getValue());

        StringAttrConstraint directionality = attr.getStringAttrConstraint(Point2PointTypes.DIRECTIONALITY);
        assertNotNull(directionality);
        assertEquals(DirectionalityType.BIDIRECTIONAL, DirectionalityType.valueOf(directionality.getValue()));

        BooleanAttrConstraint symmetricPath = attr.getBooleanAttrConstraint(Point2PointTypes.SYMMETRICPATH);
        assertNotNull(symmetricPath);
        assertEquals(Boolean.TRUE, symmetricPath.getValue());

        StringAttrConstraint sourceSTP = attr.getStringAttrConstraint(Point2PointTypes.SOURCESTP);
        assertNotNull(sourceSTP);
        assertEquals("urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:uva:1?vlan=1784", sourceSTP.getValue());

        StringAttrConstraint destSTP = attr.getStringAttrConstraint(Point2PointTypes.DESTSTP);
        assertNotNull(destSTP);
        assertEquals("urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:pionier:1?vlan=1784", destSTP.getValue());

        StringAttrConstraint vlan = attr.getStringAttrConstraint(EthernetTypes.VLAN);
        assertNull(vlan);
    }
}
