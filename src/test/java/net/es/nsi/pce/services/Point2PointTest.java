/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.services;

import java.util.Set;
import net.es.nsi.pce.api.jaxb.DirectionalityType;
import net.es.nsi.pce.api.jaxb.EthernetVlanType;
import net.es.nsi.pce.api.jaxb.StpType;
import net.es.nsi.pce.pf.api.cons.AttrConstraint;
import net.es.nsi.pce.pf.api.cons.CapacityConstraint;
import net.es.nsi.pce.pf.api.cons.Constraint;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class Point2PointTest {
    @Test
    public void testEVTS() {
        // We want an EVTS service for this test.
        EthernetVlanType evts = new EthernetVlanType();
        evts.setCapacity(100L);
        evts.setDirectionality(DirectionalityType.BIDIRECTIONAL);
        evts.setSymmetricPath(Boolean.TRUE);
        
        // Format the source STP.
        StpType srcStp = new StpType();
        srcStp.setLocalId("i2-edge");
        srcStp.setNetworkId("urn:ogf:network:internet2.edu");
        evts.setSourceSTP(srcStp);
        evts.setSourceVLAN(1780);
        
        // Format the destination STP.
        StpType destStp = new StpType();
        destStp.setLocalId("esnet-edge-one");
        destStp.setNetworkId("urn:ogf:network:es.net");
        evts.setDestSTP(destStp);
        evts.setDestVLAN(1780);
        
        Set<Constraint> result = Point2Point.getConstraints(evts);
        assertNotNull(result);
        assertEquals(4, result.size());
        
        for (Constraint constraint : result) {
            if (constraint instanceof AttrConstraint) {
                AttrConstraint attrContraint = (AttrConstraint) constraint;
                System.out.println("constraint name = " + attrContraint.getAttrName());
                if (attrContraint.getAttrName().equalsIgnoreCase(CapacityConstraint.CAPACITY)) {
                    CapacityConstraint capacity = (CapacityConstraint) attrContraint;
                    assertEquals(100, capacity.getValue().longValue());
                }
            }
        }
    }
}
