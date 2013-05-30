package net.es.nsi.pce.svc;


import net.es.nsi.pce.svc.api.*;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;


import javax.ws.rs.core.Response;
import java.util.ArrayList;

public class FindPathServiceImpl implements FindPathService {

    public Response findPath(FindPathRequest request) {
        System.out.println("----invoking findPath, correlation id is: " + request.correlationId);

        Response r = Response.ok().build();

        String aggUrl = request.replyTo;

        FindPathResponse resp = new FindPathResponse();
        resp.path = this.fakePath(request.sourceStp, request.destinationStp);
        resp.correlationId = request.correlationId;
        resp.status = FindPathStatus.SUCCESS;
        resp.message = "all good!";

        // terrible hack
        aggUrl = aggUrl.replace("pathreply", "");


        AggService agg = JAXRSClientFactory.create(aggUrl, AggService.class);
        agg.pathReply(resp);


        return r;
    }

    private ArrayList<PathObject> fakePath(StpObject src, StpObject dst)  {
        AuthObject ao = new AuthObject();
        ao.method = AuthMethod.NONE;

        ArrayList<PathObject> path = new ArrayList<PathObject>();
        if (src.networkId.equals(dst.networkId)) {


            PathObject pa = new PathObject();
            pa.sourceStp = src;
            pa.destinationStp = dst;
            pa.nsa = src.networkId;
            pa.providerUrl = src.networkId;
            pa.auth = ao;
            path.add(pa);

        } else {
            StpObject b = new StpObject();
            b.networkId = src.networkId;
            b.localId = b.networkId+"-B";

            StpObject y = new StpObject();
            y.networkId = dst.networkId;
            y.localId = y.networkId+"-Y";


            PathObject a = new PathObject();
            a.sourceStp = src;
            a.destinationStp = b;
            a.nsa = src.networkId;
            a.providerUrl = src.networkId;
            a.auth = ao;
            path.add(a);


            PathObject z = new PathObject();
            z.sourceStp = y;
            z.destinationStp = dst;
            z.nsa = dst.networkId;
            z.providerUrl = dst.networkId;
            z.auth = ao;
            path.add(z);
        }

        return path;


    }

}