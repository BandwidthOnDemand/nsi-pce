package net.es.nsi.pce.pf;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Optional;

import net.es.nsi.pce.pf.api.PCEData;

import org.junit.Ignore;
import org.junit.Test;


public class ReachabilityPCETest {

    private ReachabilityPCE subject = new ReachabilityPCE();

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_when_missing_source_stp() {
        PCEData pceData = new PCEData();
        subject.findPath(pceData);
    }

    @Test
    @Ignore("todo")
    public void should_calculate_path_if_both_src_and_dest_is_in_topology() {
    }

    @Test
    @Ignore("todo")
    public void should_splitup_request_if_src_is_in_toplogy() {
    }

    @Test
    @Ignore("todo")
    public void should_forward_request_to_nearest_nsa() {
    }

    @Test
    public void should_find_topology_id_from_stp_id() {
        List<String> stpIds = Arrays.asList(
            "urn:ogf:network:surfnet.nl:1990:topology:foobar",
            "urn:ogf:network:surfnet.nl:1990:testbed:1",
            "urn:ogf:network:surfnet.nl:1990:testbed:foobar",
            "urn:ogf:network:surfnet.nl:1990:testbed:foo:bar",
            "urn:ogf:network:surfnet.nl:1990:testbed");

        for (String stpId : stpIds) {
            Optional<String> topologyId = subject.extractTopologyId(stpId);

            assertTrue(topologyId.isPresent());
            assertEquals("urn:ogf:network:surfnet.nl:1990", topologyId.get());
        }
    }

    @Test
    public void should_not_find_topology_id_in_empty_string() {
        Optional<String> topologyId = subject.extractTopologyId("");

        assertFalse(topologyId.isPresent());
    }

    @Test(expected = NullPointerException.class)
    public void should_throw_nullpointer_when_stp_id_is_null() {
        subject.extractTopologyId(null);
    }
}
