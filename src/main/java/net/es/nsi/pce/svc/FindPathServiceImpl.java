package net.es.nsi.pce.svc;


import net.es.nsi.pce.pf.PathfinderCore;
import net.es.nsi.pce.svc.api.*;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;


import javax.ws.rs.core.Response;

public class FindPathServiceImpl implements FindPathService {

    public Response findPath(FindPathRequest request) {
        System.out.println("----invoking findPath, correlation id is: " + request.correlationId);

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
            resp.path = null;
            resp.status = FindPathStatus.FAILED;
            resp.message = "oops!";
            r = Response.status(Response.Status.BAD_REQUEST).build();
        }

        // terrible hack
        aggUrl = aggUrl.replace("pathreply", "");


        AggService agg = JAXRSClientFactory.create(aggUrl, AggService.class);
        agg.pathReply(resp);


        return r;
    }


}