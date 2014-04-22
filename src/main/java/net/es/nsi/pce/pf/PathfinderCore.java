package net.es.nsi.pce.pf;

import java.util.List;
import java.util.Set;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.topology.provider.TopologyProvider;
import net.es.nsi.pce.path.jaxb.FindPathAlgorithmType;
import net.es.nsi.pce.pf.api.NsiError;
import net.es.nsi.pce.pf.api.Path;
import net.es.nsi.pce.pf.api.cons.Constraint;
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
    private TopologyProvider topologyProvider;
    private net.es.nsi.pce.pf.SequentialPCE chainPCE;
    private net.es.nsi.pce.pf.SequentialPCE treePCE;
    /**
     * Default constructor for this class.  Performs lookups on the Spring
     * beans for required services.
     *
     * @throws Exception If Spring cannot resolve the desired beans.
     */
    public PathfinderCore(TopologyProvider topologyProvider, net.es.nsi.pce.pf.SequentialPCE chainPCE, net.es.nsi.pce.pf.SequentialPCE treePCE) {
        this.topologyProvider = topologyProvider;
        this.chainPCE = chainPCE;
        this.treePCE = treePCE;
    }

    /**
     * This class builds a path finding request, loads the appropriate path
     * finding modules, issues the path finding request, collects the
     * results, and builds a list of NSA and request segments.
     *
     * @param algorithm The algorithm to run for this path request (CHAIN or TREE).
     * @param contraints The list of path constraints.
     * @param po The list to populate the path results.
     * @return Resolved path if one exists, otherwise an exception.
     * @throws Exception An exception carrying an XML encoded findPathErrorType.
     */
    public Path findPath(FindPathAlgorithmType algorithm, Set<Constraint> contraints, List<String> trace) throws Exception {
        // Build the path computation request.
        PCEData pceData = new PCEData();

        // Add routing constrains.
        pceData.getConstraints().addAll(contraints);

        // Add topology
        pceData.setTopology(topologyProvider.getTopology());
        
        log.debug("******* findPath " + pceData.getTopology().getLocalNsaId());
        for (String networkId : topologyProvider.getTopology().getNetworkIds()) {
            log.debug("******* networkId " + networkId);
        }
        
        // Add trace.
        pceData.setTrace(trace);

        // Determine the path computation module to invoke.
        PCEModule pce;
        if (algorithm != null && algorithm.equals(FindPathAlgorithmType.CHAIN)) {
            pce = chainPCE;
        }
        else {
            pce = treePCE;
        }

        // Invoke the path computation sequence on this request.
        PCEData result = pce.apply(pceData);

        // TODO: I don't think this can ever occur.
        if (result == null || result.getPath() == null) {
            String error = NsiError.getFindPathErrorString(NsiError.NO_PATH_FOUND, "PCECore", "No path found using provided criteria");
            throw new Exception(error);
        }

        return result.getPath();
    }
}