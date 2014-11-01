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
import org.junit.Before;

import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;

/**
 *
 * @author hacksaw
 */
public class NmlEthernetTest {

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void labelTest() throws Exception {
        NmlLabelGroupType mockLG = mock(NmlLabelGroupType.class);
        when(mockLG.getLabeltype()).thenReturn("http://schemas.ogf.org/nml/2012/10/ethernet#vlan");
        when(mockLG.getValue()).thenReturn("200-299,1801-1821");

        Optional<String> labelType = Optional.fromNullable(mockLG.getLabeltype());
        assertTrue(NmlEthernet.isVlanLabel(labelType));

        Set<NmlLabelType> labels = NmlEthernet.labelGroupToLabels(mockLG);
        assertEquals(121, labels.size());
        for (NmlLabelType label : labels) {
            Integer integer = Integer.parseInt(label.getValue());
            assertTrue(integer > 199 && integer < 300 || integer > 1800 && integer < 1822);
        }
    }
}

