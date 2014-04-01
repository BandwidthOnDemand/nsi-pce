package net.es.nsi.pce.pf.api;

import net.es.nsi.pce.pf.api.cons.Constraint;
import net.es.nsi.pce.topology.model.NsiTopology;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Defines the data passed into and between Path Computation Modules.
 *
 * path - contains the computed path so far.  Each PC module can further
 * refine the path for the next PC module in the sequence.
 *
 * constraints - contains list of constraints to guide path finding.  Constraints
 * will contain user requested parameters such as source and destination STP,
 * as well as vlan tags, ERO, etc.  It can also contain additional constraints
 * added by the system.
 *
 * topology - the current network topology utilized for pathfinding.
 *
 * @author hacksaw
 */
public class PCEData {
    private Path path = new Path();
    private Set<Constraint> constraints = new HashSet<>();
    private NsiTopology topology = new NsiTopology();
    private Map<String, Map<String, Long>> reachabilityTable;

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public Set<Constraint> getConstraints() {
        return constraints;
    }

    public NsiTopology getTopology() {
        return topology;
    }

    public void setTopology(NsiTopology topology) {
        this.topology = topology;
    }

    public Map<String, Map<String, Long>> getReachabilityTable() {
        return reachabilityTable;
    }

    public void setReachabilityTable(Map<String, Map<String, Long>> reachabilityTable) {
        this.reachabilityTable = reachabilityTable;
    }
}
