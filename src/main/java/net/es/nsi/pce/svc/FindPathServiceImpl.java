package net.es.nsi.pce.svc;


import net.es.nsi.pce.pf.PathfinderCore;
import net.es.nsi.pce.svc.api.*;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;


import javax.ws.rs.core.Response;

public class FindPathServiceImpl implements FindPathService {

    public Response findPath(FindPathRequest request) {
        System.out.println("----invoking findPath");
        System.out.println("correlation id: " + request.correlationId);
        System.out.println("replyto: "+request.replyTo);
        System.out.println("src-local: "+request.sourceStp.localId);
        System.out.println("src-net: "+request.sourceStp.networkId);
        System.out.println("dst-local: "+request.destinationStp.localId);
        System.out.println("dst-net: "+request.destinationStp.networkId);
        System.out.println("algorithm: "+request.algorithm);
        System.out.println("bandwidth: "+request.bandwidth);
        System.out.println("startTime: "+request.startTime);
        System.out.println("endtime: "+request.endTime);


        Response r = Response.status(Response.Status.ACCEPTED).build();

        String aggUrl = request.replyTo;
        FindPathResponse resp = new FindPathResponse();
        resp.correlationId = request.correlationId;

        PathfinderCore pathfinderCore = new PathfinderCore();
        try {
            resp.path = pathfinderCore.findPath(request.sourceStp, request.destinationStp, request.algorithm);
            resp.status = FindPathStatus.SUCCESS;
            resp.message = "all good!";
        } catch (Exception ex) {
            ex.printStackTrace();
            resp.path = null;
            resp.status = FindPathStatus.FAILED;
            resp.message = "oops!";
            r = Response.status(Response.Status.BAD_REQUEST).build();
        }

        AggService agg = JAXRSClientFactory.create(aggUrl, AggService.class);
        agg.pathReply(resp);

        return r;
    }


}