package net.es.nsi.pce.pf;

import java.util.List;
import java.util.Set;
import javax.ws.rs.WebApplicationException;
import net.es.nsi.pce.jaxb.path.FindPathAlgorithmType;
import net.es.nsi.pce.path.api.Exceptions;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.Path;
import net.es.nsi.pce.pf.api.cons.Constraint;
import net.es.nsi.pce.topology.provider.TopologyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles requests for path finding from the RESTful web service.
 * Parameters are validated as much as possible before issuing to the specific
 * path find algorithms.
 * @author hacksaw
 */
public class PathfinderCore {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final TopologyProvider topologyProvider;
    private final net.es.nsi.pce.pf.SequentialPCE chainPCE;
    private final net.es.nsi.pce.pf.SequentialPCE treePCE;
    private final net.es.nsi.pce.pf.SequentialPCE sequentialPCE;

    /**
     * Default constructor for this class.  Performs lookups on the Spring
     * beans for required services.
     *
     * @param topologyProvider
     * @param chainPCE
     * @param treePCE
     * @param sequentialPCE
     */
    public PathfinderCore(TopologyProvider topologyProvider,
            net.es.nsi.pce.pf.SequentialPCE chainPCE,
            net.es.nsi.pce.pf.SequentialPCE treePCE,
            net.es.nsi.pce.pf.SequentialPCE sequentialPCE) {
        this.topologyProvider = topologyProvider;
        this.chainPCE = chainPCE;
        this.treePCE = treePCE;
        this.sequentialPCE = sequentialPCE;
    }

    /**
     * This class builds a path finding request, loads the appropriate path
     * finding modules, issues the path finding request, collects the
     * results, and builds a list of NSA and request segments.
     *
     * @param algorithm The algorithm to run for this path request (CHAIN or TREE).
     * @param contraints The list of path constraints.
     * @param trace
     * @return Resolved path if one exists, otherwise an exception.
     * @throws WebApplicationException
     */
    public Path findPath(FindPathAlgorithmType algorithm, Set<Constraint> contraints, List<String> trace) throws WebApplicationException {
        // Build the path computation request.
        PCEData pceData = new PCEData();

        // Add routing constrains.
        pceData.addConstraints(contraints);

        // Add topology
        pceData.setTopology(topologyProvider.getTopology());

        // Add trace.
        pceData.setTrace(trace);

        // Determine the path computation module to invoke.
        PCEModule pce = treePCE;
        if (algorithm != null) {
            if (algorithm.equals(FindPathAlgorithmType.CHAIN)) {
                pce = chainPCE;
            }
            else if (algorithm.equals(FindPathAlgorithmType.SEQUENTIAL)) {
                pce = sequentialPCE;
            }
        }

        // Invoke the path computation sequence on this request.
        PCEData result = pce.apply(pceData);

        // TODO: I don't think this can ever occur.
        if (result == null || result.getPath() == null) {
            throw Exceptions.noPathFound("No path found using provided criteria");
        }

        return result.getPath();
    }
}