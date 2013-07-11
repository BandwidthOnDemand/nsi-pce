package net.es.nsi.pce.test.client;

import static org.testng.Assert.assertEquals;

import java.util.Date;
import java.util.UUID;

import javax.ws.rs.core.Response;

import net.es.nsi.pce.config.SpringContext;
import net.es.nsi.pce.svc.AggServer;
import net.es.nsi.pce.svc.PCEServer;
import net.es.nsi.pce.svc.api.FindPathAlgorithm;
import net.es.nsi.pce.svc.api.FindPathRequest;
import net.es.nsi.pce.svc.api.FindPathService;
import net.es.nsi.pce.svc.api.StpObject;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.springframework.context.ApplicationContext;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;


public class FindPathTest {
    String testUrl = "http://localhost:8600/";
    String testAggUrl = "http://localhost:8601/";
    private ApplicationContext context;

    @BeforeSuite(groups="pf")
    public void init() throws Exception {
        SpringContext sc = SpringContext.getInstance();
        context = sc.initContext("src/test/resources/config/beans.xml");
        PCEServer.getInstance(testUrl, "testJetty.xml");
        AggServer.getInstance(testAggUrl, "testJetty.xml");

        Thread.sleep(1000);
    }

    @Test(groups="pf")
    public void TestClient() throws Exception {
        StpObject srcStp = new StpObject();
        srcStp.localId = "i2-edge";
        srcStp.networkId = "urn:ogf:network:stp:internet2.edu";

        StpObject destStp = new StpObject();
        destStp.localId = "esnet-edge-one";
        destStp.networkId = "urn:ogf:network:stp:es.net";

        Date now = new Date();
        Date tenmin = new Date(now.getTime() + 10*60*1000);

        FindPathService pce = JAXRSClientFactory.create(testUrl, FindPathService.class);
        FindPathRequest req = new FindPathRequest();
        req.correlationId = UUID.randomUUID().toString();
        req.algorithm = FindPathAlgorithm.CHAIN;
        req.bandwidth = 100L;
        req.destinationStp = destStp;
        req.sourceStp = srcStp;
        req.replyTo = testAggUrl+"pathreply/";

        req.startTime = now;
        req.endTime = tenmin;

        Response findPath = pce.findPath(req);

        assertEquals(findPath.getStatus(), 202);
    }

}
