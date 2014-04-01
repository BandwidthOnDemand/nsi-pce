package net.es.nsi.pce.pf;

import static org.junit.Assert.assertEquals;
import net.es.nsi.pce.pf.api.PCEData;

import org.junit.Test;


public class ReachabilityPCETest {

    private ReachabilityPCE subject = new ReachabilityPCE();

    @Test
    public void shouldDoNothingYet() throws Exception {
        PCEData initialData = new PCEData();
        PCEData resultingData = subject.apply(initialData);

        assertEquals(initialData, resultingData);
    }
}
