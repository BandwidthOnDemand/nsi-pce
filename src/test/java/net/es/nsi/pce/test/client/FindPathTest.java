package net.es.nsi.pce.test.client;

import net.es.nsi.pce.api.FindPathAlgorithm;
import net.es.nsi.pce.api.FindPathRequest;
import net.es.nsi.pce.api.StpObject;
import net.es.nsi.pce.api.FindPathService;
import net.es.nsi.pce.common.PCEServer;
import net.es.nsi.pce.common.AggServer;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.UUID;


public class FindPathTest {
    String testUrl = "http://localhost:8600/";
    String testAggUrl = "http://localhost:8601/";
    @BeforeSuite
    public void init() throws Exception {
        PCEServer s = PCEServer.getInstance(testUrl, "testJetty.xml");

        AggServer a = AggServer.getInstance(testAggUrl, "testJetty.xml");

        Thread.sleep(1000);
    }


    @Test
    public void TestClient() throws Exception {
        StpObject srcStp = new StpObject();
        srcStp.localId = "internet2.edu-A";
        srcStp.networkId = "internet2.edu";

        StpObject destStp = new StpObject();
        destStp.localId = "es.net-Z";
        destStp.networkId = "es.net";



        Date now = new Date();
        Date tenmin = new Date(now.getTime() + 10*60*1000 );

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


        pce.findPath(req);




    }

}
