/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.topology.model;

import java.util.Set;
import com.google.common.base.Optional;
import net.es.nsi.pce.topology.jaxb.NmlLabelGroupType;
import net.es.nsi.pce.topology.jaxb.NmlLabelType;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author hacksaw
 */
public class NmlEthernetTest {
    @Test
    public void labelTest() throws Exception {
        NmlLabelGroupType lg = new NmlLabelGroupType();
        lg.setLabeltype("http://schemas.ogf.org/nml/2012/10/ethernet#vlan");
        lg.setValue("200-299,1801-1821");
        Optional<String> labelType = Optional.fromNullable(lg.getLabeltype());
        assertTrue(NmlEthernet.isVlanLabel(labelType));
        Set<NmlLabelType> labels = NmlEthernet.labelGroupToLabels(lg);
        assertEquals(121, labels.size());
        for (NmlLabelType label : labels) {
            System.out.println(label.getValue());
        }
    }
}
