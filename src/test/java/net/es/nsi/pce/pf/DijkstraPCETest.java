package net.es.nsi.pce.pf;

import java.util.Arrays;
import java.util.List;

import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.topology.jaxb.DemarcationType;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.model.NsiTopology;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;


public class DijkstraPCETest {

    private DijkstraPCE subject = new DijkstraPCE();

    @Test
    public void should_pull_out_individual_segments() {
        StpType srcStp = new StpType();
        srcStp.setNetworkId("urn:ogf:network:surfnet.nl:1990:testbed");
        srcStp.setId("urn:ogf:network:surfnet.nl:1990:testbed:start");

        StpType dstStp = new StpType();
        dstStp.setNetworkId("urn:ogf:network:surfnet.nl:1990:testbed");
        dstStp.setId("urn:ogf:network:surfnet.nl:1990:testbed:end");

        StpType intermediateStp = new StpType();
        intermediateStp.setNetworkId("urn:ogf:network:surfnet.nl:1990:testbed");
        intermediateStp.setId("urn:ogf:network:surfnet.nl:1990:testbed:1");

        List<SdpType> path = Arrays.asList(
            createEdge("urn:ogf:network:surfnet.nl:1990:testbed:start", "urn:ogf:network:surfnet.nl:1990:testbed:1"),
            createEdge("urn:ogf:network:surfnet.nl:1990:testbed:1", "urn:ogf:network:surfnet.nl:1990:testbed:end"));

        NsiTopology nsiTopology = new NsiTopology();
        nsiTopology.addAllStp(Arrays.asList(srcStp, dstStp, intermediateStp));

        List<StpPair> segments = subject.pullIndividualSegmentsOut(srcStp, dstStp, path, nsiTopology);

        assertThat(segments.size(), is(3));
        assertThat(segments.get(0).getA().getId(), is("urn:ogf:network:surfnet.nl:1990:testbed:start"));
        assertThat(segments.get(2).getZ().getId(), is("urn:ogf:network:surfnet.nl:1990:testbed:end"));
    }

    private SdpType createEdge(String start, String end) {
        SdpType edge = new SdpType();
        ResourceRefType resourceRefA = new ResourceRefType();
        resourceRefA.setId(start);
        DemarcationType demarcationA = new DemarcationType();
        demarcationA.setStp(resourceRefA);
        ResourceRefType resourceRefB = new ResourceRefType();
        resourceRefB.setId(end);
        DemarcationType demarcationB = new DemarcationType();
        demarcationB.setStp(resourceRefB);

        edge.setDemarcationA(demarcationA);
        edge.setDemarcationZ(demarcationB);

        return edge;
    }
}
